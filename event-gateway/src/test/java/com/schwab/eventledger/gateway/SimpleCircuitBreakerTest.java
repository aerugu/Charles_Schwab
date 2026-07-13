package com.schwab.eventledger.gateway;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

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

    private AccountServiceProperties properties(int threshold, long openMs) {
        return new AccountServiceProperties("http://localhost:8081", 100, 1, 1, threshold, openMs);
    }

    private Clock fixedClock(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }
}
