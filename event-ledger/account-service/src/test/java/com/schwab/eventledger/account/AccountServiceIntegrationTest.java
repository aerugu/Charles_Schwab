package com.schwab.eventledger.account;

import com.schwab.eventledger.common.AccountTransactionRequest;
import com.schwab.eventledger.common.BalanceResponse;
import com.schwab.eventledger.common.EventType;
import com.schwab.eventledger.common.HealthResponse;
import com.schwab.eventledger.common.MetricsSnapshot;
import com.schwab.eventledger.common.TraceHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountServiceIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DataSource dataSource;

    @Test
    void usesDedicatedAccountServiceDatabase() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).contains("accountdb");
        }
    }

    @Test
    void healthReturnsServiceStatusAndDatabaseDiagnostics() {
        var health = restTemplate.getForObject("/health", HealthResponse.class);

        assertThat(health.service()).isEqualTo("account-service");
        assertThat(health.status()).isEqualTo("UP");
        assertThat(health.timestamp()).isNotNull();
        assertThat(health.diagnostics()).containsEntry("database", "UP");
        assertThat(health.diagnostics()).containsKey("transactionRows");
    }

    @Test
    void appliesTransactionsIdempotentlyAndComputesBalanceRegardlessOfOrder() {
        post("acct-balance", new AccountTransactionRequest(
                "acct-test-002", EventType.DEBIT, new BigDecimal("25.00"), "USD", Instant.parse("2026-05-15T14:02:11Z")));
        post("acct-balance", new AccountTransactionRequest(
                "acct-test-001", EventType.CREDIT, new BigDecimal("150.00"), "USD", Instant.parse("2026-05-15T13:02:11Z")));
        post("acct-balance", new AccountTransactionRequest(
                "acct-test-001", EventType.CREDIT, new BigDecimal("150.00"), "USD", Instant.parse("2026-05-15T13:02:11Z")));

        var balance = restTemplate.getForObject("/accounts/acct-balance/balance", BalanceResponse.class);

        assertThat(balance.balance()).isEqualByComparingTo("125.00");
    }

    @Test
    void metricsExposeRequestAndErrorCountsByEndpointTemplate() {
        restTemplate.getForObject("/accounts/acct-metrics/balance", BalanceResponse.class);
        restTemplate.postForEntity("/accounts/acct-metrics/transactions", Map.of(), String.class);

        var metrics = restTemplate.getForObject("/metrics", MetricsSnapshot.class);

        assertThat(metrics.service()).isEqualTo("account-service");
        assertThat(metrics.requestCounts().get("GET /accounts/{accountId}/balance")).isGreaterThanOrEqualTo(1L);
        assertThat(metrics.errorCounts().get("POST /accounts/{accountId}/transactions")).isGreaterThanOrEqualTo(1L);
    }

    private void post(String accountId, AccountTransactionRequest request) {
        var headers = new HttpHeaders();
        headers.set(TraceHeaders.TRACE_ID, "account-test-trace");
        restTemplate.postForEntity("/accounts/{accountId}/transactions", new HttpEntity<>(request, headers), Void.class, accountId);
    }
}
