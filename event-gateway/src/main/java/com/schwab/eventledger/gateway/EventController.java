package com.schwab.eventledger.gateway;

import com.schwab.eventledger.common.AccountDetailsResponse;
import com.schwab.eventledger.common.BalanceResponse;
import com.schwab.eventledger.common.EventResponse;
import com.schwab.eventledger.common.HealthResponse;
import com.schwab.eventledger.common.MetricsSnapshot;
import com.schwab.eventledger.common.TransactionEventRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
class EventController {
    private final EventService eventService;
    private final EventRepository eventRepository;
    private final AccountClient accountClient;
    private final ServiceMetrics metrics;

    EventController(EventService eventService, EventRepository eventRepository, AccountClient accountClient, ServiceMetrics metrics) {
        this.eventService = eventService;
        this.eventRepository = eventRepository;
        this.accountClient = accountClient;
        this.metrics = metrics;
    }

    @PostMapping("/events")
    ResponseEntity<EventResponse> submit(@Valid @RequestBody TransactionEventRequest request) {
        var result = eventService.submit(request);
        return result.duplicate()
                ? ResponseEntity.ok(result.response())
                : ResponseEntity.status(201).body(result.response());
    }

    @GetMapping("/events/{eventId}")
    EventResponse event(@PathVariable String eventId) {
        return eventService.event(eventId);
    }

    @GetMapping("/events")
    List<EventResponse> events(@RequestParam("account") String accountId) {
        return eventService.eventsForAccount(accountId);
    }

    @GetMapping("/accounts/{accountId}/balance")
    BalanceResponse balance(@PathVariable String accountId) {
        return accountClient.balance(accountId);
    }

    @GetMapping("/accounts/{accountId}")
    AccountDetailsResponse account(@PathVariable String accountId) {
        return accountClient.account(accountId);
    }

    @GetMapping("/health")
    HealthResponse health() {
        boolean databaseUp = eventRepository.databaseAvailable();
        return new HealthResponse("event-gateway", databaseUp ? "UP" : "DOWN", Instant.now(), Map.of(
                "database", databaseUp ? "UP" : "DOWN",
                "eventRows", databaseUp ? eventRepository.countRows() : "unavailable",
                "accountServiceCircuitOpen", accountClient.circuitOpen()
        ));
    }

    @GetMapping("/metrics")
    MetricsSnapshot metrics() {
        return metrics.snapshot();
    }
}
