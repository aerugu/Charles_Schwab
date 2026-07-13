package com.schwab.eventledger.gateway;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
class SimpleCircuitBreaker {
    private enum State {
        CLOSED,
        OPEN
    }

    private final AccountServiceProperties properties;
    private final Clock clock;
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private volatile Instant openedAt = Instant.EPOCH;

    @org.springframework.beans.factory.annotation.Autowired
    SimpleCircuitBreaker(AccountServiceProperties properties) {
        this(properties, Clock.systemUTC());
    }

    SimpleCircuitBreaker(AccountServiceProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    void beforeCall() {
        if (state.get() == State.OPEN) {
            var elapsed = Instant.now(clock).toEpochMilli() - openedAt.toEpochMilli();
            if (elapsed < properties.circuitOpenMs()) {
                throw new AccountUnavailableException("Account Service circuit is open; try again shortly");
            }
            state.set(State.CLOSED);
            consecutiveFailures.set(0);
        }
    }

    void recordSuccess() {
        consecutiveFailures.set(0);
        state.set(State.CLOSED);
    }

    void recordFailure() {
        if (consecutiveFailures.incrementAndGet() >= properties.circuitFailureThreshold()) {
            openedAt = Instant.now(clock);
            state.set(State.OPEN);
        }
    }

    boolean isOpen() {
        return state.get() == State.OPEN;
    }
}
