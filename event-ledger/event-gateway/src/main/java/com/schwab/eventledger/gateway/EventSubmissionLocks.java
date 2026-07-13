package com.schwab.eventledger.gateway;

import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Component
class EventSubmissionLocks {
    private static final int STRIPE_COUNT = 256;
    private final ReentrantLock[] locks = new ReentrantLock[STRIPE_COUNT];

    EventSubmissionLocks() {
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    <T> T withEventLock(String eventId, Supplier<T> work) {
        var lock = locks[Math.floorMod(eventId.hashCode(), locks.length)];
        lock.lock();
        try {
            return work.get();
        } finally {
            lock.unlock();
        }
    }
}
