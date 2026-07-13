package com.schwab.eventledger.gateway;

import com.schwab.eventledger.common.EventType;
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

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("account-service.base-url", () -> "http://localhost:1");
        registry.add("account-service.timeout-ms", () -> "100");
        registry.add("account-service.max-attempts", () -> "2");
        registry.add("account-service.backoff-ms", () -> "10");
        registry.add("account-service.circuit-failure-threshold", () -> "2");
        registry.add("account-service.circuit-open-ms", () -> "5000");
    }

    @Test
    void returnsServiceUnavailableWhenAccountServiceIsDownAndGatewayReadsStillWork() {
        var response1 = restTemplate.postForEntity("/events", event("resilience-evt-001"), String.class);
        var response2 = restTemplate.postForEntity("/events", event("resilience-evt-002"), String.class);
        var response3 = restTemplate.postForEntity("/events", event("resilience-evt-003"), String.class);

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response3.getBody()).contains("circuit is open");

        var localRead = restTemplate.getForEntity("/events?account=acct-resilience", String.class);

        assertThat(localRead.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(localRead.getBody()).isEqualTo("[]");
    }

    private TransactionEventRequest event(String eventId) {
        return new TransactionEventRequest(
                eventId,
                "acct-resilience",
                EventType.CREDIT,
                new BigDecimal("10.00"),
                "USD",
                Instant.parse("2026-05-15T14:02:11Z"),
                Map.of()
        );
    }
}
