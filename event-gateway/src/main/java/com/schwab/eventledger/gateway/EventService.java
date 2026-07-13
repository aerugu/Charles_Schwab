package com.schwab.eventledger.gateway;

import com.schwab.eventledger.common.AccountTransactionRequest;
import com.schwab.eventledger.common.EventResponse;
import com.schwab.eventledger.common.TransactionEventRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class EventService {
    private final EventRepository repository;
    private final AccountClient accountClient;

    EventService(EventRepository repository, AccountClient accountClient) {
        this.repository = repository;
        this.accountClient = accountClient;
    }

    SubmissionResult submit(TransactionEventRequest request) {
        var existing = repository.findById(request.eventId());
        if (existing.isPresent()) {
            return new SubmissionResult(existing.get().toResponse(true), true);
        }

        accountClient.applyTransaction(request.accountId(), new AccountTransactionRequest(
                request.eventId(),
                request.type(),
                request.amount(),
                request.currency(),
                request.eventTimestamp()
        ));
        return new SubmissionResult(repository.save(request).toResponse(false), false);
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

    record SubmissionResult(EventResponse response, boolean duplicate) {
    }
}
