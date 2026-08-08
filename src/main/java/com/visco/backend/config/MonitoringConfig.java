package com.visco.backend.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.registry.otlp.OtlpConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MonitoringConfig {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCustomizer(
            @Value("${management.metrics.tags.application}") String app,
            @Value("${management.metrics.tags.instance}") String instance) {
        return registry -> registry.config()
                .commonTags("application", app, "instance", instance);
    }

    @Bean
    @ConditionalOnProperty(name = "management.otlp.metrics.export.enabled", havingValue = "true")
    OtlpConfig otlpRegistryConfig(
            @Value("${management.otlp.metrics.export.url}") String url,
            @Value("${GRAFANA_INSTANCE_ID:3341928}") String instanceId,
            @Value("${GCLOUD_RW_API_KEY:}") String apiKey) {
        return new OtlpConfig() {
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public String url() {
                return url;
            }

            @Override
            public Map<String, String> headers() {
                Map<String, String> headers = new HashMap<>();
                if (!apiKey.isBlank()) {
                    String credentials = instanceId + ":" + apiKey;
                    String basic = Base64.getEncoder()
                            .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
                    headers.put("Authorization", "Basic " + basic);
                }
                return headers;
            }
        };
    }
}
