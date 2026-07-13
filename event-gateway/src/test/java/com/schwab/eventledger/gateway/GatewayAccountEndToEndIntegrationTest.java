package com.schwab.eventledger.gateway;

import com.schwab.eventledger.account.AccountServiceApplication;
import com.schwab.eventledger.common.BalanceResponse;
import com.schwab.eventledger.common.EventResponse;
import com.schwab.eventledger.common.EventType;
import com.schwab.eventledger.common.TraceHeaders;
import com.schwab.eventledger.common.TransactionEventRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayAccountEndToEndIntegrationTest {
    private final RestTemplate restTemplate = new RestTemplate();
    private ConfigurableApplicationContext accountContext;
    private ConfigurableApplicationContext gatewayContext;

    @AfterEach
    void stopServices() {
        if (gatewayContext != null) {
            gatewayContext.close();
        }
        if (accountContext != null) {
            accountContext.close();
        }
    }

    @Test
    void gatewaySubmitsEventToRealAccountServiceAndReturnsComputedBalance() {
        var runId = UUID.randomUUID().toString();
        accountContext = startAccountService(runId);
        gatewayContext = startGateway(runId, serviceUrl(accountContext));

        var request = new TransactionEventRequest(
                "e2e-evt-001",
                "acct-e2e",
                EventType.CREDIT,
                new BigDecimal("150.00"),
                "USD",
                Instant.parse("2026-05-15T14:02:11Z"),
                Map.of("source", "end-to-end-test")
        );
        var headers = new HttpHeaders();
        headers.set(TraceHeaders.TRACE_ID, "trace-e2e-001");

        var created = restTemplate.postForEntity(
                serviceUrl(gatewayContext) + "/events",
                new HttpEntity<>(request, headers),
                EventResponse.class
        );
        var duplicate = restTemplate.postForEntity(
                serviceUrl(gatewayContext) + "/events",
                new HttpEntity<>(request, headers),
                EventResponse.class
        );
        var balance = restTemplate.getForEntity(
                serviceUrl(gatewayContext) + "/accounts/acct-e2e/balance",
                BalanceResponse.class
        );

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().duplicate()).isFalse();
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(duplicate.getBody()).isNotNull();
        assertThat(duplicate.getBody().duplicate()).isTrue();
        assertThat(balance.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(balance.getBody()).isNotNull();
        assertThat(balance.getBody().balance()).isEqualByComparingTo("150.00");
    }

    private ConfigurableApplicationContext startAccountService(String runId) {
        return new SpringApplicationBuilder(AccountServiceApplication.class)
                .run(
                        "--spring.application.name=account-service",
                        "--server.port=0",
                        "--spring.datasource.url=jdbc:h2:mem:account-e2e-" + runId + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password="
                );
    }

    private ConfigurableApplicationContext startGateway(String runId, String accountServiceUrl) {
        return new SpringApplicationBuilder(EventGatewayApplication.class)
                .run(
                        "--spring.application.name=event-gateway",
                        "--server.port=0",
                        "--spring.datasource.url=jdbc:h2:mem:gateway-e2e-" + runId + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--account-service.base-url=" + accountServiceUrl,
                        "--account-service.timeout-ms=1000",
                        "--account-service.max-attempts=2",
                        "--account-service.backoff-ms=10",
                        "--account-service.circuit-failure-threshold=3",
                        "--account-service.circuit-open-ms=5000"
                );
    }

    private String serviceUrl(ConfigurableApplicationContext context) {
        return "http://localhost:" + ((WebServerApplicationContext) context).getWebServer().getPort();
    }
}
