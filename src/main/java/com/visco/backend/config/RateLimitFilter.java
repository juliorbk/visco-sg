package com.visco.backend.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate-limiting filter using the token-bucket algorithm (Bucket4j) to
 * throttle requests per IP address on a per-route basis. Limits are
 * configured through {@code rate-limit.*} application properties. Applied
 * before Spring Security's authentication filter.
 *
 * <p>Buckets are stored in a bounded Caffeine cache with {@code expireAfterAccess}
 * so that idle (IP, route) entries are evicted automatically without the
 * periodic sweep a plain {@code ConcurrentHashMap} would otherwise need.
 * A {@code maximumSize} cap protects the JVM from attackers who keep
 * generating new source IPs to inflate the map.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(10))
        .maximumSize(50_000)
        .build();

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private enum RateLimitedRoute {
        LOGIN("/api/auth/login", "rate-limit.login"),
        REGISTER("/api/auth/register", "rate-limit.register"),
        PASSWORD_RESET("/api/auth/forgot-password", "rate-limit.password-reset"),
        ADMIN_OPERATIONS("/api/admin/**", "rate-limit.admin"),
        INVENTORY("/api/inventory/**", "rate-limit.inventory"),
        PROCUREMENT("/api/procurement/**", "rate-limit.procurement"),
        USERS("/api/users/**", "rate-limit.users"),
        SUPPLIERS("/api/suppliers/**", "rate-limit.suppliers"),
        WAREHOUSES("/api/warehouse/**", "rate-limit.warehouses"),
        DASHBOARD("/api/dashboard/**", "rate-limit.dashboard"),
        INVOICES("/api/invoices/**", "rate-limit.invoices"),
        REQUISITIONS("/api/requisitions/**", "rate-limit.requisitions"),
        COST_CENTERS("/api/cost-centers/**", "rate-limit.cost-centers"),
        EMPLOYEES("/api/employees/**", "rate-limit.employees"),
        REPORTS("/api/reports/**", "rate-limit.reports");

        final String pattern;
        final String propertyPrefix;

        RateLimitedRoute(String pattern, String propertyPrefix) {
            this.pattern = pattern;
            this.propertyPrefix = propertyPrefix;
        }
    }

    @Value("${rate-limit.login.capacity:8}")
    private int loginCapacity;
    @Value("${rate-limit.login.minutes:1}")
    private int loginMinutes;
    @Value("${rate-limit.register.capacity:5}")
    private int registerCapacity;
    @Value("${rate-limit.register.minutes:5}")
    private int registerMinutes;
    @Value("${rate-limit.password-reset.capacity:3}")
    private int passwordResetCapacity;
    @Value("${rate-limit.password-reset.minutes:5}")
    private int passwordResetMinutes;
    @Value("${rate-limit.admin.capacity:30}")
    private int adminCapacity;
    @Value("${rate-limit.admin.minutes:1}")
    private int adminMinutes;
    @Value("${rate-limit.inventory.capacity:60}")
    private int inventoryCapacity;
    @Value("${rate-limit.inventory.minutes:1}")
    private int inventoryMinutes;
    @Value("${rate-limit.procurement.capacity:60}")
    private int procurementCapacity;
    @Value("${rate-limit.procurement.minutes:1}")
    private int procurementMinutes;
    @Value("${rate-limit.users.capacity:30}")
    private int usersCapacity;
    @Value("${rate-limit.users.minutes:1}")
    private int usersMinutes;
    @Value("${rate-limit.suppliers.capacity:60}")
    private int suppliersCapacity;
    @Value("${rate-limit.suppliers.minutes:1}")
    private int suppliersMinutes;
    @Value("${rate-limit.warehouses.capacity:60}")
    private int warehousesCapacity;
    @Value("${rate-limit.warehouses.minutes:1}")
    private int warehousesMinutes;
    @Value("${rate-limit.dashboard.capacity:30}")
    private int dashboardCapacity;
    @Value("${rate-limit.dashboard.minutes:1}")
    private int dashboardMinutes;
    @Value("${rate-limit.invoices.capacity:60}")
    private int invoicesCapacity;
    @Value("${rate-limit.invoices.minutes:1}")
    private int invoicesMinutes;
    @Value("${rate-limit.requisitions.capacity:60}")
    private int requisitionsCapacity;
    @Value("${rate-limit.requisitions.minutes:1}")
    private int requisitionsMinutes;
    @Value("${rate-limit.cost-centers.capacity:30}")
    private int costCentersCapacity;
    @Value("${rate-limit.cost-centers.minutes:1}")
    private int costCentersMinutes;
    @Value("${rate-limit.employees.capacity:30}")
    private int employeesCapacity;
    @Value("${rate-limit.employees.minutes:1}")
    private int employeesMinutes;
    @Value("${rate-limit.reports.capacity:30}")
    private int reportsCapacity;
    @Value("${rate-limit.reports.minutes:1}")
    private int reportsMinutes;
    @Value("${rate-limit.trust-forwarded-for:false}")
    private boolean trustForwardedFor;

    private int capacityFor(RateLimitedRoute route) {
        return switch (route) {
            case LOGIN -> loginCapacity;
            case REGISTER -> registerCapacity;
            case PASSWORD_RESET -> passwordResetCapacity;
            case ADMIN_OPERATIONS -> adminCapacity;
            case INVENTORY -> inventoryCapacity;
            case PROCUREMENT -> procurementCapacity;
            case USERS -> usersCapacity;
            case SUPPLIERS -> suppliersCapacity;
            case WAREHOUSES -> warehousesCapacity;
            case DASHBOARD -> dashboardCapacity;
            case INVOICES -> invoicesCapacity;
            case REQUISITIONS -> requisitionsCapacity;
            case COST_CENTERS -> costCentersCapacity;
            case EMPLOYEES -> employeesCapacity;
            case REPORTS -> reportsCapacity;
        };
    }

    private int minutesFor(RateLimitedRoute route) {
        return switch (route) {
            case LOGIN -> loginMinutes;
            case REGISTER -> registerMinutes;
            case PASSWORD_RESET -> passwordResetMinutes;
            case ADMIN_OPERATIONS -> adminMinutes;
            case INVENTORY -> inventoryMinutes;
            case PROCUREMENT -> procurementMinutes;
            case USERS -> usersMinutes;
            case SUPPLIERS -> suppliersMinutes;
            case WAREHOUSES -> warehousesMinutes;
            case DASHBOARD -> dashboardMinutes;
            case INVOICES -> invoicesMinutes;
            case REQUISITIONS -> requisitionsMinutes;
            case COST_CENTERS -> costCentersMinutes;
            case EMPLOYEES -> employeesMinutes;
            case REPORTS -> reportsMinutes;
        };
    }

    private Bucket createBucket(RateLimitedRoute route) {
        return Bucket.builder()
            .addLimit(
                Bandwidth.builder()
                    .capacity(capacityFor(route))
                    .refillIntervally(
                        capacityFor(route),
                        Duration.ofMinutes(minutesFor(route))
                    )
                    .build()
            )
            .build();
    }

    private String resolveIp(HttpServletRequest request) {
        // Solo se confía en X-Forwarded-For si está explícitamente activado
        // (típicamente cuando hay un proxy reverso confiable como Render/Cloudflare).
        // Por default NO se confía para evitar IP spoofing.
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * Matches the request URI against rate-limited routes, consumes a token
     * from the per-IP bucket, and either proceeds with the filter chain or
     * responds with 429 Too Many Requests when the bucket is exhausted.
     */
    @Override
    public void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String uri = request.getRequestURI();

        RateLimitedRoute matchedRoute = null;
        for (RateLimitedRoute route : RateLimitedRoute.values()) {
            if (PATH_MATCHER.match(route.pattern, uri)) {
                matchedRoute = route;
                break;
            }
        }

        if (matchedRoute == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = resolveIp(request);
        String bucketKey = ip + ":" + matchedRoute.name();
        RateLimitedRoute finalRoute = matchedRoute;
        Bucket bucket = buckets.get(bucketKey, k -> createBucket(finalRoute));

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
