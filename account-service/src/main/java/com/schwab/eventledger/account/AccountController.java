package com.schwab.eventledger.account;

import com.schwab.eventledger.common.AccountDetailsResponse;
import com.schwab.eventledger.common.AccountTransactionRequest;
import com.schwab.eventledger.common.BalanceResponse;
import com.schwab.eventledger.common.HealthResponse;
import com.schwab.eventledger.common.MetricsSnapshot;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Internal REST API for account state.
 *
 * <p>The Account Service owns transaction application and balance computation.
 * It is intentionally not exposed directly by Docker Compose; callers go
 * through the Event Gateway.</p>
 */
@RestController
class AccountController {
    private final TransactionRepository repository;
    private final ServiceMetrics metrics;
    private final JsonLogger logger;

    AccountController(TransactionRepository repository, ServiceMetrics metrics, JsonLogger logger) {
        this.repository = repository;
        this.metrics = metrics;
        this.logger = logger;
    }

    @PostMapping("/accounts/{accountId}/transactions")
    ResponseEntity<Void> applyTransaction(@PathVariable String accountId, @Valid @RequestBody AccountTransactionRequest request) {
        boolean created = repository.apply(accountId, request);
        logger.info("transaction_apply_result", Map.of(
                "accountId", accountId,
                "eventId", request.eventId(),
                "created", created
        ));
        return created ? ResponseEntity.status(HttpStatus.CREATED).build() : ResponseEntity.ok().build();
    }

    @GetMapping("/accounts/{accountId}/balance")
    BalanceResponse balance(@PathVariable String accountId) {
        return new BalanceResponse(accountId, repository.balance(accountId), repository.currency(accountId).orElse("USD"));
    }

    @GetMapping("/accounts/{accountId}")
    AccountDetailsResponse account(@PathVariable String accountId) {
        return new AccountDetailsResponse(
                accountId,
                repository.balance(accountId),
                repository.currency(accountId).orElse("USD"),
                repository.recentTransactions(accountId)
        );
    }

    @GetMapping("/health")
    HealthResponse health() {
        boolean databaseUp = repository.databaseAvailable();
        return new HealthResponse("account-service", databaseUp ? "UP" : "DOWN", Instant.now(), Map.of(
                "database", databaseUp ? "UP" : "DOWN",
                "transactionRows", databaseUp ? repository.countRows() : "unavailable"
        ));
    }

    @GetMapping("/metrics")
    MetricsSnapshot metrics() {
        return metrics.snapshot();
    }
}
