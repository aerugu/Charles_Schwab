package com.schwab.eventledger.gateway;

import com.schwab.eventledger.common.AccountDetailsResponse;
import com.schwab.eventledger.common.AccountTransactionRequest;
import com.schwab.eventledger.common.BalanceResponse;
import com.schwab.eventledger.common.TraceHeaders;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * REST client for the Gateway to Account Service boundary.
 *
 * <p>All calls propagate the current trace ID and are protected by timeout,
 * bounded retry, exponential backoff with jitter, and a circuit breaker. The
 * client converts transport failures into {@link AccountUnavailableException}
 * so controllers and background workers can make consistent degradation
 * decisions.</p>
 */
@Component
class AccountClient {
    private final RestTemplate restTemplate;
    private final AccountServiceProperties properties;
    private final SimpleCircuitBreaker circuitBreaker;
    private final JsonLogger logger;

    AccountClient(RestTemplateBuilder builder, AccountServiceProperties properties,
                  SimpleCircuitBreaker circuitBreaker, JsonLogger logger) {
        this.properties = properties;
        this.circuitBreaker = circuitBreaker;
        this.logger = logger;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(properties.timeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.timeoutMs()))
                .rootUri(properties.baseUrl())
                .build();
    }

    void applyTransaction(String accountId, AccountTransactionRequest request) {
        executeWithResilience(() -> {
            var headers = new HttpHeaders();
            headers.set(TraceHeaders.TRACE_ID, TraceContext.get());
            restTemplate.exchange(
                    "/accounts/{accountId}/transactions",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    Void.class,
                    accountId
            );
            return null;
        }, "apply_transaction");
    }

    BalanceResponse balance(String accountId) {
        return executeWithResilience(() -> requireBody(exchange(
                "/accounts/{accountId}/balance",
                HttpMethod.GET,
                null,
                BalanceResponse.class,
                accountId
        )), "get_balance");
    }

    AccountDetailsResponse account(String accountId) {
        return executeWithResilience(() -> requireBody(exchange(
                "/accounts/{accountId}",
                HttpMethod.GET,
                null,
                AccountDetailsResponse.class,
                accountId
        )), "get_account");
    }

    boolean circuitOpen() {
        return circuitBreaker.isOpen();
    }

    RestTemplate restTemplate() {
        return restTemplate;
    }

    private <T> T executeWithResilience(CheckedSupplier<T> supplier, String operation) {
        circuitBreaker.beforeCall();
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            try {
                T result = supplier.get();
                circuitBreaker.recordSuccess();
                return result;
            } catch (RestClientException | AccountUnavailableException ex) {
                lastFailure = ex;
                logger.warn("account_service_call_failed", Map.of(
                        "operation", operation,
                        "attempt", attempt,
                        "maxAttempts", properties.maxAttempts(),
                        "error", ex.getClass().getSimpleName()
                ));
                if (attempt < properties.maxAttempts()) {
                    sleep(backoffDelay(attempt));
                }
            }
        }
        circuitBreaker.recordFailure();
        throw new AccountUnavailableException("Account Service is unavailable for operation " + operation, lastFailure);
    }

    private long backoffDelay(int attempt) {
        var exponentialMultiplier = 1L << Math.min(attempt - 1, 10);
        var baseDelay = properties.backoffMs() * exponentialMultiplier;
        var jitter = properties.jitterMs() == 0 ? 0 : ThreadLocalRandom.current().nextLong(properties.jitterMs() + 1);
        return baseDelay + jitter;
    }

    private <T> ResponseEntity<T> exchange(String path, HttpMethod method, Object body, Class<T> responseType, Object... uriVariables) {
        var headers = new HttpHeaders();
        headers.set(TraceHeaders.TRACE_ID, TraceContext.get());
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), responseType, uriVariables);
    }

    private <T> T requireBody(ResponseEntity<T> response) {
        var body = response.getBody();
        if (body == null) {
            throw new AccountUnavailableException("Account Service returned an empty response body");
        }
        return body;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AccountUnavailableException("Interrupted while retrying Account Service call", e);
        }
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get();
    }
}
