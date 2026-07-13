package com.schwab.eventledger.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schwab.eventledger.common.BalanceResponse;
import com.schwab.eventledger.common.EventResponse;
import com.schwab.eventledger.common.EventType;
import com.schwab.eventledger.common.HealthResponse;
import com.schwab.eventledger.common.MetricsSnapshot;
import com.schwab.eventledger.common.TraceHeaders;
import com.schwab.eventledger.common.TransactionEventRequest;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayIntegrationTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final CopyOnWriteArrayList<String> TRACE_IDS = new CopyOnWriteArrayList<>();
    private static final AtomicInteger TRANSACTION_CALLS = new AtomicInteger();
    private static HttpServer accountStub;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AccountServiceProperties accountServiceProperties;

    @BeforeEach
    void resetAccountStubState() {
        TRACE_IDS.clear();
        TRANSACTION_CALLS.set(0);
    }

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
        registry.add("account-service.jitter-ms", () -> "0");
        registry.add("gateway.rate-limit.enabled", () -> "false");
        registry.add("gateway.pending-retry.enabled", () -> "false");
        registry.add("gateway.pending-retry.interval-ms", () -> "5000");
        registry.add("gateway.pending-retry.batch-size", () -> "25");
    }

    @Test
    void usesDedicatedGatewayDatabaseAndAccountServiceRestBoundary() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).contains("gatewaydb");
        }
        assertThat(accountServiceProperties.baseUrl())
                .isEqualTo("http://localhost:" + accountStub.getAddress().getPort());
    }

    @Test
    void healthReturnsServiceStatusAndDiagnostics() {
        var health = restTemplate.getForObject("/health", HealthResponse.class);

        assertThat(health.service()).isEqualTo("event-gateway");
        assertThat(health.status()).isEqualTo("UP");
        assertThat(health.timestamp()).isNotNull();
        assertThat(health.diagnostics()).containsEntry("database", "UP");
        assertThat(health.diagnostics()).containsKey("eventRows");
        assertThat(health.diagnostics()).containsKey("accountServiceCircuitOpen");
    }

    @Test
    void acceptsEventsIdempotentlyListsChronologicallyAndPropagatesTraceIds() {
        var later = event("gw-evt-002", "acct-gateway", EventType.DEBIT, "25.00", "2026-05-15T14:02:11Z");
        var earlier = event("gw-evt-001", "acct-gateway", EventType.CREDIT, "150.00", "2026-05-15T13:02:11Z");
        var reusedIdDifferentPayload = event("gw-evt-001", "acct-gateway", EventType.DEBIT, "999.00", "2026-05-15T15:02:11Z");

        var laterResponse = post(later, "trace-gateway-123");
        var earlierResponse = post(earlier, "trace-gateway-456");
        var duplicateResponse = post(reusedIdDifferentPayload, "trace-gateway-789");

        assertThat(laterResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(earlierResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(duplicateResponse.getBody().duplicate()).isTrue();
        assertThat(duplicateResponse.getBody().type()).isEqualTo(EventType.CREDIT);
        assertThat(duplicateResponse.getBody().amount()).isEqualByComparingTo("150.00");
        assertThat(TRANSACTION_CALLS.get()).isEqualTo(2);
        assertThat(TRACE_IDS).contains("trace-gateway-123", "trace-gateway-456");

        var events = restTemplate.getForObject("/events?account=acct-gateway", EventResponse[].class);

        assertThat(events).extracting(EventResponse::eventId).containsExactly("gw-evt-001", "gw-evt-002");
    }

    @Test
    void generatesTraceIdWhenMissingAndPropagatesItToAccountService() {
        var request = event("gw-generated-trace-001", "acct-generated-trace", EventType.CREDIT, "15.00", "2026-05-15T14:02:11Z");

        var response = restTemplate.postForEntity("/events", request, EventResponse.class);
        var generatedTraceId = response.getHeaders().getFirst(TraceHeaders.TRACE_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(generatedTraceId).isNotBlank().matches("[0-9a-f]{32}");
        assertThat(TRACE_IDS).containsExactly(generatedTraceId);
    }

    @Test
    void rejectsInvalidEventsWithMeaningfulBadRequest() {
        var zeroAmount = new TransactionEventRequest(
                "gw-invalid",
                "acct-invalid",
                EventType.CREDIT,
                BigDecimal.ZERO,
                "USD",
                Instant.parse("2026-05-15T14:02:11Z"),
                Map.of()
        );

        var zeroAmountResponse = restTemplate.postForEntity("/events", zeroAmount, String.class);
        var missingRequiredResponse = restTemplate.postForEntity("/events", Map.of(
                "eventId", "gw-missing",
                "amount", "15.00",
                "currency", "USD",
                "eventTimestamp", "2026-05-15T14:02:11Z"
        ), String.class);
        var unknownTypeResponse = restTemplate.postForEntity("/events", Map.of(
                "eventId", "gw-unknown-type",
                "accountId", "acct-invalid",
                "type", "TRANSFER",
                "amount", "15.00",
                "currency", "USD",
                "eventTimestamp", "2026-05-15T14:02:11Z"
        ), String.class);

        assertThat(zeroAmountResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(zeroAmountResponse.getBody()).contains("amount", "greater than 0");
        assertThat(missingRequiredResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(missingRequiredResponse.getBody()).contains("accountId", "type");
        assertThat(unknownTypeResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(unknownTypeResponse.getBody()).contains("type must be CREDIT or DEBIT");
    }

    @Test
    void proxiesBalanceQueriesToAccountService() {
        var response = restTemplate.getForEntity("/accounts/acct-gateway/balance", BalanceResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().balance()).isEqualByComparingTo("125.00");
    }

    @Test
    void metricsExposeRequestAndErrorCountsByEndpointTemplate() {
        restTemplate.getForEntity("/accounts/acct-gateway/balance", BalanceResponse.class);
        restTemplate.postForEntity("/events", Map.of(), String.class);

        var metrics = restTemplate.getForObject("/metrics", MetricsSnapshot.class);

        assertThat(metrics.service()).isEqualTo("event-gateway");
        assertThat(metrics.requestCounts().get("GET /accounts/{accountId}/balance")).isGreaterThanOrEqualTo(1L);
        assertThat(metrics.errorCounts().get("POST /events")).isGreaterThanOrEqualTo(1L);
    }

    private ResponseEntity<EventResponse> post(TransactionEventRequest request, String traceId) {
        var headers = new HttpHeaders();
        headers.set(TraceHeaders.TRACE_ID, traceId);
        return restTemplate.postForEntity("/events", new HttpEntity<>(request, headers), EventResponse.class);
    }

    private static TransactionEventRequest event(String eventId, String accountId, EventType type, String amount, String timestamp) {
        return new TransactionEventRequest(
                eventId,
                accountId,
                type,
                new BigDecimal(amount),
                "USD",
                Instant.parse(timestamp),
                Map.of("source", "integration-test")
        );
    }

    private static void handleAccountRequest(HttpExchange exchange) throws IOException {
        TRACE_IDS.add(exchange.getRequestHeaders().getFirst(TraceHeaders.TRACE_ID));
        if ("POST".equals(exchange.getRequestMethod()) && exchange.getRequestURI().getPath().endsWith("/transactions")) {
            TRANSACTION_CALLS.incrementAndGet();
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
            return;
        }
        if ("GET".equals(exchange.getRequestMethod()) && exchange.getRequestURI().getPath().endsWith("/balance")) {
            var body = OBJECT_MAPPER.writeValueAsString(new BalanceResponse("acct-gateway", new BigDecimal("125.00"), "USD"));
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.getBytes(StandardCharsets.UTF_8).length);
            exchange.getResponseBody().write(body.getBytes(StandardCharsets.UTF_8));
            exchange.close();
            return;
        }
        exchange.sendResponseHeaders(404, -1);
        exchange.close();
    }

    private static synchronized void ensureAccountStubStarted() {
        if (accountStub != null) {
            return;
        }
        try {
            accountStub = HttpServer.create(new InetSocketAddress(0), 0);
            accountStub.createContext("/accounts", GatewayIntegrationTest::handleAccountRequest);
            accountStub.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
