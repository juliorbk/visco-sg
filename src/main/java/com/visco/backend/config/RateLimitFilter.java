package com.visco.backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
  private final Map<String, Long> lastAccessTime = new ConcurrentHashMap<>();

  private static final long CLEANUP_INTERVAL_MS = 300000;
  private static final long STALE_THRESHOLD_MS = 600000;
  private long lastCleanup = System.currentTimeMillis();
  private final Object cleanupLock = new Object();

  private enum RateLimitedRoute {
    LOGIN("/api/auth/login", 8, 1),
    REGISTER("/api/auth/register", 3, 5),
    ADMIN_OPERATIONS("/api/admin", 30, 1),
    //INVENTORY("/api/inventory", 200, 1),
    PROCUREMENT("/api/procurement", 60, 1),
    USERS("/api/users", 30, 1),
    SUPPLIERS("/api/suppliers", 60, 1),
    WAREHOUSES("/api/warehouse", 60, 1),
    DASHBOARD("/api/dashboard", 30, 1);

    final String path;
    final int capacity;
    final int minutes;

    RateLimitedRoute(String path, int capacity, int minutes) {
      this.path = path;
      this.capacity = capacity;
      this.minutes = minutes;
    }
  }

  private Bucket createBucket(RateLimitedRoute route) {
    return Bucket.builder()
      .addLimit(
        Bandwidth.builder()
          .capacity(route.capacity)
          .refillIntervally(route.capacity, Duration.ofMinutes(route.minutes))
          .build()
      )
      .build();
  }

  private void cleanupOldBuckets() {
    long now = System.currentTimeMillis();
    if (now - lastCleanup > CLEANUP_INTERVAL_MS) {
      synchronized (cleanupLock) {
        buckets
          .keySet()
          .removeIf(key -> {
            Long lastAccess = lastAccessTime.get(key);
            return (
              lastAccess != null && (now - lastAccess > STALE_THRESHOLD_MS)
            );
          });
        lastAccessTime.keySet().removeIf(key -> !buckets.containsKey(key));
        lastCleanup = now;
      }
    }
  }

  private String resolveIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim(); // solo el primer IP, sanitizado
    }
    return request.getRemoteAddr();
  }

  @Override
  public void doFilterInternal(
    @NonNull HttpServletRequest request,
    @NonNull HttpServletResponse response,
    @NonNull FilterChain filterChain
  ) throws ServletException, IOException {
    String uri = request.getRequestURI();

    RateLimitedRoute matchedRoute = null;
    for (RateLimitedRoute route : RateLimitedRoute.values()) {
      if (uri.startsWith(route.path)) {
        matchedRoute = route;
        break;
      }
    }

    if (matchedRoute == null) {
      filterChain.doFilter(request, response);
      return;
    }

    cleanupOldBuckets();

    String ip = resolveIp(request);
    String bucketKey = ip + ":" + matchedRoute.name();
    lastAccessTime.put(bucketKey, System.currentTimeMillis());
    RateLimitedRoute finalRoute = matchedRoute;
    Bucket bucket = buckets.computeIfAbsent(bucketKey, k ->
      createBucket(finalRoute)
    );

    if (bucket.tryConsume(1)) {
      filterChain.doFilter(request, response);
    } else {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType("application/json");
      response
        .getWriter()
        .write(
          "{\"error\": \"Demasiadas solicitudes. Espera un momento e intenta de nuevo.\"}"
        );
    }
  }
}
