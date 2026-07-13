package com.schwab.eventledger.gateway;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Gateway edge rate limiter implemented as an in-memory token bucket.
 *
 * <p>This protects the exercise service from sustained request bursts without
 * introducing an external dependency. A production multi-instance deployment
 * would typically move this concern to an API gateway or Redis-backed limiter.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimitProperties properties;
    private final AtomicReference<Bucket> bucket;

    RateLimitFilter(RateLimitProperties properties) {
        this.properties = properties;
        this.bucket = new AtomicReference<>(new Bucket(properties.capacity(), Instant.now().toEpochMilli()));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled()
                || request.getRequestURI().equals("/health")
                || request.getRequestURI().startsWith("/metrics");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (tryAcquire()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"error":"Too Many Requests","messages":["Gateway rate limit exceeded; retry later"]}
                """);
    }

    private boolean tryAcquire() {
        while (true) {
            var current = bucket.get();
            var now = Instant.now().toEpochMilli();
            var elapsedMs = Math.max(0, now - current.refilledAtMs());
            var refill = (elapsedMs * properties.refillPerSecond()) / 1000;
            var tokens = Math.min(properties.capacity(), current.tokens() + refill);
            var refilledAt = refill > 0 ? now : current.refilledAtMs();
            if (tokens <= 0) {
                return false;
            }
            if (bucket.compareAndSet(current, new Bucket(tokens - 1, refilledAt))) {
                return true;
            }
        }
    }

    private record Bucket(long tokens, long refilledAtMs) {
    }
}
