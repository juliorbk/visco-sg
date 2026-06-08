package com.visco.backend;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Main entry point for the Visco SG backend application. Configures core
 * beans: caching, scheduling, async execution, Spring Data web support with
 * DTO serialization mode, and a dedicated {@link TaskExecutor} for email
 * delivery.
 */
@EnableCaching
@EnableScheduling
@EnableAsync
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class SystemApplication {

  public static void main(String[] args) {
    SpringApplication.run(SystemApplication.class, args);
  }

  // Add this method to explicitly provide the ObjectMapper bean
  /**
   * Provides the default {@link ObjectMapper} bean for JSON serialization
   * and deserialization across the application.
   *
   * @return the object mapper bean
   */
  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  /**
   * Dedicated executor for outbound email tasks so SMTP latency never
   * blocks request threads. Used implicitly by {@code @Async} on
   * {@code EmailService}.
   */
  @Bean(name = "emailExecutor")
  public TaskExecutor emailExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("email-");
    executor.setKeepAliveSeconds(60);
    executor.initialize();
    return executor;
  }
}
