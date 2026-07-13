package com.schwab.eventledger.gateway;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimpleCircuitBreakerTest {
    @Test
    void opensAfterConfiguredFailureThresholdAndFailsFast() {
        var breaker = new SimpleCircuitBreaker(properties(2, 5_000), fixedClock("2026-05-15T14:02:11Z"));

        breaker.recordFailure();
        assertThat(breaker.isOpen()).isFalse();

        breaker.recordFailure();
        assertThat(breaker.isOpen()).isTrue();
        assertThatThrownBy(breaker::beforeCall)
                .isInstanceOf(AccountUnavailableException.class)
                .hasMessageContaining("circuit is open");
    }

    @Test
    void successClosesCircuitAndResetsFailureCount() {
        var breaker = new SimpleCircuitBreaker(properties(2, 5_000), fixedClock("2026-05-15T14:02:11Z"));

        breaker.recordFailure();
        breaker.recordSuccess();
        breaker.recordFailure();

        assertThat(breaker.isOpen()).isFalse();
    }

    @Test
    void allowsTrialCallAfterOpenWindowExpires() {
        var now = new AtomicReference<>(Instant.parse("2026-05-15T14:02:11Z"));
        var breaker = new SimpleCircuitBreaker(properties(1, 5_000), mutableClock(now));

        breaker.recordFailure();
        assertThat(breaker.isOpen()).isTrue();

        now.set(Instant.parse("2026-05-15T14:02:17Z"));

        breaker.beforeCall();
        assertThat(breaker.isOpen()).isFalse();
    }

    private AccountServiceProperties properties(int threshold, long openMs) {
        return new AccountServiceProperties("http://localhost:8081", 100, 1, 1, 0, threshold, openMs);
    }

    private Clock fixedClock(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }

    private Clock mutableClock(AtomicReference<Instant> now) {
        return new Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now.get();
            }
        };
    }
}
