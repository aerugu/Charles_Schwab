package com.schwab.eventledger.account;

import com.schwab.eventledger.common.MetricsSnapshot;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * In-memory request/error counters exposed by the Account Service metrics API.
 */
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
        return new MetricsSnapshot("account-service", snapshot(requestCounts), snapshot(errorCounts));
    }

    private Map<String, Long> snapshot(Map<String, LongAdder> source) {
        return source.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().sum()));
    }
}
