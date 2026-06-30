package com.visco.backend.services;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MemoryMonitorService {

    private final MeterRegistry meterRegistry;

    public MemoryMonitorService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void registerGauges() {
        Gauge.builder("jvm.memory.heap.used_percent", this,
                m -> {
                    Runtime rt = Runtime.getRuntime();
                    long used = rt.totalMemory() - rt.freeMemory();
                    return (double) used / rt.maxMemory() * 100;
                })
                .description("Heap usage percentage")
                .register(meterRegistry);
    }

    @Scheduled(fixedRate = 600_000)
    void logMemoryMetrics() {
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        long max = rt.maxMemory();
        double pct = (double) used / max * 100;

        log.atInfo()
                .setMessage("Memory snapshot")
                .addKeyValue("heapTotalMB", rt.totalMemory() / 1_048_576)
                .addKeyValue("heapFreeMB", rt.freeMemory() / 1_048_576)
                .addKeyValue("heapUsedMB", used / 1_048_576)
                .addKeyValue("heapMaxMB", max / 1_048_576)
                .addKeyValue("heapUsedPercent", String.format("%.1f", pct))
                .log();
    }
}
