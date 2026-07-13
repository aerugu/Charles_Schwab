package com.schwab.eventledger.gateway;

import com.schwab.eventledger.common.AccountTransactionRequest;
import com.schwab.eventledger.common.EventResponse;
import com.schwab.eventledger.common.TransactionEventRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Coordinates event submission semantics for the Gateway.
 *
 * <p>The service is responsible for preserving idempotency before invoking the
 * Account Service. When the downstream account dependency is unavailable, the
 * event remains stored locally and is marked pending for asynchronous retry.</p>
 */
@Service
class EventService {
    private final EventRepository repository;
    private final AccountClient accountClient;
    private final EventSubmissionLocks submissionLocks;

    EventService(EventRepository repository, AccountClient accountClient, EventSubmissionLocks submissionLocks) {
        this.repository = repository;
        this.accountClient = accountClient;
        this.submissionLocks = submissionLocks;
    }

    SubmissionResult submit(TransactionEventRequest request) {
        return submissionLocks.withEventLock(request.eventId(), () -> submitLocked(request));
    }

    private SubmissionResult submitLocked(TransactionEventRequest request) {
        var existing = repository.findById(request.eventId());
        if (existing.isPresent()) {
            return new SubmissionResult(existing.get().toResponse(true), true, false);
        }

        var saveAttempt = repository.saveOrFindExisting(request);
        if (!saveAttempt.created()) {
            return new SubmissionResult(saveAttempt.record().toResponse(true), true, false);
        }

        try {
            accountClient.applyTransaction(request.accountId(), new AccountTransactionRequest(
                    request.eventId(),
                    request.type(),
                    request.amount(),
                    request.currency(),
                    request.eventTimestamp()
            ));
            return new SubmissionResult(saveAttempt.record().toResponse(false), false, false);
        } catch (AccountUnavailableException ex) {
            repository.markPending(request.eventId(), ex.getMessage());
            return new SubmissionResult(saveAttempt.record().toResponse(false), false, true);
        }
    }

    EventResponse event(String eventId) {
        return repository.findById(eventId)
                .map(record -> record.toResponse(false))
                .orElseThrow(() -> new EventNotFoundException(eventId));
    }

    List<EventResponse> eventsForAccount(String accountId) {
        return repository.findByAccount(accountId).stream()
                .map(record -> record.toResponse(false))
                .toList();
    }

    record SubmissionResult(EventResponse response, boolean duplicate, boolean pending) {
    }
}
