package com.schwab.eventledger.account;

import com.schwab.eventledger.common.TraceHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
class TraceAndMetricsFilter extends OncePerRequestFilter {
    private final ServiceMetrics metrics;
    private final JsonLogger logger;

    TraceAndMetricsFilter(ServiceMetrics metrics, JsonLogger logger) {
        this.metrics = metrics;
        this.logger = logger;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var traceId = request.getHeader(TraceHeaders.TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = "missing-trace-id";
        }
        TraceContext.set(traceId);
        response.setHeader(TraceHeaders.TRACE_ID, traceId);
        var started = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            var route = request.getMethod() + " " + request.getRequestURI();
            metrics.record(route, response.getStatus());
            logger.info("request_completed", Map.of(
                    "method", request.getMethod(),
                    "path", request.getRequestURI(),
                    "status", response.getStatus(),
                    "durationMs", (System.nanoTime() - started) / 1_000_000
            ));
            TraceContext.clear();
        }
    }
}
