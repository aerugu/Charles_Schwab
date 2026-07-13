package com.schwab.eventledger.gateway;

import com.schwab.eventledger.common.MetricsSnapshot;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

@Component
class ServiceMetrics {
    private final Map<String, LongAdder> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> errorCounts = new ConcurrentHashMap<>();

    void record(String route, int status) {
        requestCounts.computeIfAbsent(route, ignored -> new LongAdder()).increment();
        if (status >= 400) {
            errorCounts.computeIfAbsent(route, ignored -> new LongAdder()).increment();
        }
    }

    MetricsSnapshot snapshot() {
        return new MetricsSnapshot("event-gateway", snapshot(requestCounts), snapshot(errorCounts));
    }

    private Map<String, Long> snapshot(Map<String, LongAdder> source) {
        return source.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().sum()));
    }
}
