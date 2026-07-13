package com.schwab.eventledger.gateway;

import com.schwab.eventledger.common.AccountTransactionRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Scheduled processor for the Gateway pending outbox.
 *
 * <p>When synchronous application to the Account Service fails, the Gateway
 * stores the event and records a pending outbox row. This processor periodically
 * replays due rows and removes them only after Account Service accepts the
 * idempotent transaction.</p>
 */
@Component
class PendingAccountEventProcessor {
    private final EventRepository repository;
    private final AccountClient accountClient;
    private final PendingEventRetryProperties properties;
    private final JsonLogger logger;

    PendingAccountEventProcessor(EventRepository repository, AccountClient accountClient,
                                 PendingEventRetryProperties properties, JsonLogger logger) {
        this.repository = repository;
        this.accountClient = accountClient;
        this.properties = properties;
        this.logger = logger;
    }

    @Scheduled(fixedDelayString = "${gateway.pending-retry.interval-ms:5000}")
    void processDueEvents() {
        if (!properties.enabled()) {
            return;
        }
        var dueEvents = repository.findDuePending(Instant.now(), properties.batchSize());
        for (var pending : dueEvents) {
            apply(pending);
        }
    }

    private void apply(EventRepository.PendingEventRecord pending) {
        var event = pending.event();
        try {
            accountClient.applyTransaction(event.accountId(), new AccountTransactionRequest(
                    event.eventId(),
                    event.type(),
                    event.amount(),
                    event.currency(),
                    event.eventTimestamp()
            ));
            repository.deletePending(event.eventId());
            logger.info("pending_event_applied", Map.of(
                    "eventId", event.eventId(),
                    "accountId", event.accountId(),
                    "attempt", pending.attemptCount() + 1
            ));
        } catch (AccountUnavailableException ex) {
            var nextAttempt = pending.attemptCount() + 1;
            repository.reschedulePending(
                    event.eventId(),
                    nextAttempt,
                    Instant.now().plusMillis(retryDelay(nextAttempt)),
                    ex.getMessage()
            );
            logger.warn("pending_event_rescheduled", Map.of(
                    "eventId", event.eventId(),
                    "accountId", event.accountId(),
                    "attempt", nextAttempt,
                    "error", ex.getMessage()
            ));
        }
    }

    private long retryDelay(int attempt) {
        var multiplier = 1L << Math.min(attempt - 1, 10);
        return properties.intervalMs() * multiplier;
    }
}
