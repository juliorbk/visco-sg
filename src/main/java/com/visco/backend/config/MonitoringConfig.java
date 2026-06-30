package com.visco.backend.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MonitoringConfig {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCustomizer(
            @Value("${management.metrics.tags.application}") String app,
            @Value("${management.metrics.tags.instance}") String instance) {
        return registry -> registry.config()
                .commonTags("application", app, "instance", instance);
    }
}
