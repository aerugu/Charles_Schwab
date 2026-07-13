package com.schwab.eventledger.gateway;

import com.schwab.eventledger.common.EventType;
import com.schwab.eventledger.common.TransactionEventRequest;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayConcurrentFailureIntegrationTest {
    private static HttpServer accountStub;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeAll
    static void startAccountStub() throws IOException {
        ensureAccountStubStarted();
    }

    @AfterAll
    static void stopAccountStub() {
        if (accountStub != null) {
            accountStub.stop(0);
        }
    }

    @DynamicPropertySource
    static void accountProperties(DynamicPropertyRegistry registry) {
        ensureAccountStubStarted();
        registry.add("account-service.base-url", () -> "http://localhost:" + accountStub.getAddress().getPort());
        registry.add("account-service.timeout-ms", () -> "1000");
        registry.add("account-service.max-attempts", () -> "1");
        registry.add("account-service.jitter-ms", () -> "0");
        registry.add("account-service.circuit-failure-threshold", () -> "10");
        registry.add("gateway.rate-limit.enabled", () -> "false");
        registry.add("gateway.pending-retry.enabled", () -> "false");
        registry.add("gateway.pending-retry.interval-ms", () -> "5000");
        registry.add("gateway.pending-retry.batch-size", () -> "25");
    }

    @Test
    void concurrentDuplicateQueuesOneEventWhileOriginalApplyFails() {
        var request = event("concurrent-fail-evt-001");

        var first = CompletableFuture.supplyAsync(() -> post(request));
        var second = CompletableFuture.supplyAsync(() -> post(request));

        assertThat(first.join().getStatusCode()).isIn(HttpStatus.ACCEPTED, HttpStatus.OK);
        assertThat(second.join().getStatusCode()).isIn(HttpStatus.ACCEPTED, HttpStatus.OK);

        var localRead = restTemplate.getForEntity("/events?account=acct-concurrent-fail", String.class);

        assertThat(localRead.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(localRead.getBody()).contains("concurrent-fail-evt-001");
    }

    private ResponseEntity<String> post(TransactionEventRequest request) {
        return restTemplate.postForEntity("/events", request, String.class);
    }

    private static TransactionEventRequest event(String eventId) {
        return new TransactionEventRequest(
                eventId,
                "acct-concurrent-fail",
                EventType.CREDIT,
                new BigDecimal("10.00"),
                "USD",
                Instant.parse("2026-05-15T14:02:11Z"),
                Map.of()
        );
    }

    private static void failSlowly(HttpExchange exchange) throws IOException {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        exchange.sendResponseHeaders(503, -1);
        exchange.close();
    }

    private static synchronized void ensureAccountStubStarted() {
        if (accountStub != null) {
            return;
        }
        try {
            accountStub = HttpServer.create(new InetSocketAddress(0), 0);
            accountStub.createContext("/accounts", GatewayConcurrentFailureIntegrationTest::failSlowly);
            accountStub.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
