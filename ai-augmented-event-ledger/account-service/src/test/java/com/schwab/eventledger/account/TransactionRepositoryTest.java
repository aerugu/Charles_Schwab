package com.schwab.eventledger.account;

import com.schwab.eventledger.common.AccountTransactionRequest;
import com.schwab.eventledger.common.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionRepositoryTest {
    private TransactionRepository repository;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:account-unit-" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbcTemplate = new JdbcTemplate(dataSource);
        new AccountSchema(jdbcTemplate);
        repository = new TransactionRepository(jdbcTemplate);
    }

    @Test
    void computesCreditMinusDebitRegardlessOfArrivalOrder() {
        repository.apply("acct-unit", transaction("evt-unit-002", EventType.DEBIT, "25.00", "2026-05-15T14:02:11Z"));
        repository.apply("acct-unit", transaction("evt-unit-001", EventType.CREDIT, "150.00", "2026-05-15T13:02:11Z"));

        assertThat(repository.balance("acct-unit")).isEqualByComparingTo("125.00");
    }

    @Test
    void duplicateEventDoesNotChangeBalance() {
        var transaction = transaction("evt-duplicate", EventType.CREDIT, "150.00", "2026-05-15T13:02:11Z");

        assertThat(repository.apply("acct-unit", transaction)).isTrue();
        assertThat(repository.apply("acct-unit", transaction)).isFalse();

        assertThat(repository.balance("acct-unit")).isEqualByComparingTo("150.00");
    }

    private AccountTransactionRequest transaction(String eventId, EventType type, String amount, String timestamp) {
        return new AccountTransactionRequest(
                eventId,
                type,
                new BigDecimal(amount),
                "USD",
                Instant.parse(timestamp)
        );
    }
}
