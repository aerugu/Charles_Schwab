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

    String prometheus() {
        var builder = new StringBuilder();
        builder.append("# HELP event_gateway_http_requests_total Total HTTP requests by route.\n");
        builder.append("# TYPE event_gateway_http_requests_total counter\n");
        appendPrometheus(builder, "event_gateway_http_requests_total", requestCounts);
        builder.append("# HELP event_gateway_http_errors_total Total HTTP error responses by route.\n");
        builder.append("# TYPE event_gateway_http_errors_total counter\n");
        appendPrometheus(builder, "event_gateway_http_errors_total", errorCounts);
        return builder.toString();
    }

    private Map<String, Long> snapshot(Map<String, LongAdder> source) {
        return source.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().sum()));
    }

    private void appendPrometheus(StringBuilder builder, String metric, Map<String, LongAdder> source) {
        source.forEach((route, count) -> builder
                .append(metric)
                .append("{service=\"event-gateway\",route=\"")
                .append(escape(route))
                .append("\"} ")
                .append(count.sum())
                .append('\n'));
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
