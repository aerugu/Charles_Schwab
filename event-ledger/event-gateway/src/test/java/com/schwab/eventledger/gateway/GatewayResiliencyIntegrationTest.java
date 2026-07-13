package com.schwab.eventledger.gateway;

import com.schwab.eventledger.common.EventType;
import com.schwab.eventledger.common.EventResponse;
import com.schwab.eventledger.common.TransactionEventRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayResiliencyIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EventRepository eventRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("account-service.base-url", () -> "http://localhost:1");
        registry.add("account-service.timeout-ms", () -> "100");
        registry.add("account-service.max-attempts", () -> "2");
        registry.add("account-service.backoff-ms", () -> "10");
        registry.add("account-service.jitter-ms", () -> "0");
        registry.add("account-service.circuit-failure-threshold", () -> "2");
        registry.add("account-service.circuit-open-ms", () -> "5000");
        registry.add("gateway.rate-limit.enabled", () -> "false");
        registry.add("gateway.pending-retry.enabled", () -> "false");
        registry.add("gateway.pending-retry.interval-ms", () -> "5000");
        registry.add("gateway.pending-retry.batch-size", () -> "25");
    }

    @Test
    void returnsServiceUnavailableWhenAccountServiceIsDownAndGatewayReadsStillWork() {
        eventRepository.save(event("resilience-evt-existing", "acct-local-read"));

        var response1 = restTemplate.postForEntity("/events", event("resilience-evt-001"), String.class);
        var response2 = restTemplate.postForEntity("/events", event("resilience-evt-002"), String.class);
        var response3 = restTemplate.postForEntity("/events", event("resilience-evt-003"), String.class);

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        var balance = restTemplate.getForEntity("/accounts/acct-resilience/balance", String.class);
        assertThat(balance.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(balance.getBody()).contains("Account Service Unavailable");

        var singleEvent = restTemplate.getForEntity("/events/resilience-evt-existing", EventResponse.class);
        assertThat(singleEvent.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(singleEvent.getBody()).isNotNull();
        assertThat(singleEvent.getBody().eventId()).isEqualTo("resilience-evt-existing");

        var localRead = restTemplate.getForEntity("/events?account=acct-resilience", String.class);

        assertThat(localRead.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(localRead.getBody()).contains("resilience-evt-001", "resilience-evt-002", "resilience-evt-003");

        var existingLocalRead = restTemplate.getForEntity("/events?account=acct-local-read", EventResponse[].class);

        assertThat(existingLocalRead.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(existingLocalRead.getBody()).hasSize(1);
        assertThat(existingLocalRead.getBody()[0].eventId()).isEqualTo("resilience-evt-existing");
    }

    private TransactionEventRequest event(String eventId) {
        return event(eventId, "acct-resilience");
    }

    private TransactionEventRequest event(String eventId, String accountId) {
        return new TransactionEventRequest(
                eventId,
                accountId,
                EventType.CREDIT,
                new BigDecimal("10.00"),
                "USD",
                Instant.parse("2026-05-15T14:02:11Z"),
                Map.of()
        );
    }
}
