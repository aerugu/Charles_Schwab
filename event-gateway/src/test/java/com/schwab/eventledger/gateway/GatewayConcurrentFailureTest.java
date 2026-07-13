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
class GatewayConcurrentFailureTest {
    private static HttpServer accountStub;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeAll
    static void startAccountStub() throws IOException {
        accountStub = HttpServer.create(new InetSocketAddress(0), 0);
        accountStub.createContext("/accounts", GatewayConcurrentFailureTest::failSlowly);
        accountStub.start();
    }

    @AfterAll
    static void stopAccountStub() {
        accountStub.stop(0);
    }

    @DynamicPropertySource
    static void accountProperties(DynamicPropertyRegistry registry) {
        if (accountStub == null) {
            try {
                startAccountStub();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        registry.add("account-service.base-url", () -> "http://localhost:" + accountStub.getAddress().getPort());
        registry.add("account-service.timeout-ms", () -> "1000");
        registry.add("account-service.max-attempts", () -> "1");
        registry.add("account-service.circuit-failure-threshold", () -> "10");
    }

    @Test
    void concurrentDuplicateDoesNotReturnSuccessWhileOriginalApplyFails() {
        var request = event("concurrent-fail-evt-001");

        var first = CompletableFuture.supplyAsync(() -> post(request));
        var second = CompletableFuture.supplyAsync(() -> post(request));

        assertThat(first.join().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(second.join().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        var localRead = restTemplate.getForEntity("/events?account=acct-concurrent-fail", String.class);

        assertThat(localRead.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(localRead.getBody()).isEqualTo("[]");
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
}
