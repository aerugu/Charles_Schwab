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
    private final AuditRepository auditRepository;
    private final AccountClient accountClient;
    private final EventSubmissionLocks submissionLocks;

    EventService(EventRepository repository, AuditRepository auditRepository,
                 AccountClient accountClient, EventSubmissionLocks submissionLocks) {
        this.repository = repository;
        this.auditRepository = auditRepository;
        this.accountClient = accountClient;
        this.submissionLocks = submissionLocks;
    }

    SubmissionResult submit(TransactionEventRequest request) {
        return submissionLocks.withEventLock(request.eventId(), () -> submitLocked(request));
    }

    private SubmissionResult submitLocked(TransactionEventRequest request) {
        var existing = repository.findById(request.eventId());
        if (existing.isPresent()) {
            var existingRecord = existing.get();
            auditRepository.record(
                    existingRecord.eventId(),
                    existingRecord.accountId(),
                    AuditRepository.AuditAction.DUPLICATE_SUBMISSION,
                    "Duplicate eventId returned from Gateway ledger without reapplying transaction"
            );
            return new SubmissionResult(existingRecord.toResponse(true), true, false);
        }

        var saveAttempt = repository.saveOrFindExisting(request);
        if (!saveAttempt.created()) {
            auditRepository.record(
                    saveAttempt.record().eventId(),
                    saveAttempt.record().accountId(),
                    AuditRepository.AuditAction.DUPLICATE_SUBMISSION,
                    "Concurrent duplicate eventId returned from Gateway ledger without reapplying transaction"
            );
            return new SubmissionResult(saveAttempt.record().toResponse(true), true, false);
        }

        auditRepository.record(
                request.eventId(),
                request.accountId(),
                AuditRepository.AuditAction.EVENT_ACCEPTED,
                "Event persisted by Gateway after validation and idempotency claim"
        );
        try {
            accountClient.applyTransaction(request.accountId(), new AccountTransactionRequest(
                    request.eventId(),
                    request.type(),
                    request.amount(),
                    request.currency(),
                    request.eventTimestamp()
            ));
            auditRepository.record(
                    request.eventId(),
                    request.accountId(),
                    AuditRepository.AuditAction.ACCOUNT_APPLY_SUCCEEDED,
                    "Transaction applied successfully by Account Service"
            );
            return new SubmissionResult(saveAttempt.record().toResponse(false), false, false);
        } catch (AccountUnavailableException ex) {
            repository.markPending(request.eventId(), ex.getMessage());
            auditRepository.record(
                    request.eventId(),
                    request.accountId(),
                    AuditRepository.AuditAction.EVENT_QUEUED_FOR_RETRY,
                    ex.getMessage()
            );
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

    List<AuditEntryResponse> auditEntries(String eventId) {
        if (repository.findById(eventId).isEmpty()) {
            throw new EventNotFoundException(eventId);
        }
        return auditRepository.findByEventId(eventId);
    }

    record SubmissionResult(EventResponse response, boolean duplicate, boolean pending) {
    }
}
