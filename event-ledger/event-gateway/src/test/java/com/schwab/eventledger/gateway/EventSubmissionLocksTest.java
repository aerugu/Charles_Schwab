package com.schwab.eventledger.gateway;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class EventSubmissionLocksTest {
    @Test
    void serializesWorkForTheSameEventId() throws Exception {
        var locks = new EventSubmissionLocks();
        var firstStarted = new CountDownLatch(1);
        var releaseFirst = new CountDownLatch(1);
        var concurrentExecutions = new AtomicInteger();
        var maxConcurrentExecutions = new AtomicInteger();

        var first = CompletableFuture.runAsync(() -> locks.withEventLock("evt-same", () -> {
            firstStarted.countDown();
            trackConcurrency(concurrentExecutions, maxConcurrentExecutions);
            await(releaseFirst);
            concurrentExecutions.decrementAndGet();
            return null;
        }));

        assertThat(firstStarted.await(1, TimeUnit.SECONDS)).isTrue();

        var second = CompletableFuture.runAsync(() -> locks.withEventLock("evt-same", () -> {
            trackConcurrency(concurrentExecutions, maxConcurrentExecutions);
            concurrentExecutions.decrementAndGet();
            return null;
        }));

        Thread.sleep(100);
        assertThat(maxConcurrentExecutions.get()).isEqualTo(1);

        releaseFirst.countDown();
        first.join();
        second.join();
    }

    @Test
    void allowsWorkForDifferentEventIdsToProceedIndependently() {
        var locks = new EventSubmissionLocks();

        var first = CompletableFuture.supplyAsync(() -> locks.withEventLock("evt-a", () -> "a"));
        var second = CompletableFuture.supplyAsync(() -> locks.withEventLock("evt-b", () -> "b"));

        assertThat(first.join()).isEqualTo("a");
        assertThat(second.join()).isEqualTo("b");
    }

    private void trackConcurrency(AtomicInteger concurrentExecutions, AtomicInteger maxConcurrentExecutions) {
        var current = concurrentExecutions.incrementAndGet();
        maxConcurrentExecutions.updateAndGet(previous -> Math.max(previous, current));
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(1, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for latch");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
