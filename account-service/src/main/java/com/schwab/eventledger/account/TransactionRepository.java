package com.schwab.eventledger.account;

import com.schwab.eventledger.common.AccountDetailsResponse;
import com.schwab.eventledger.common.AccountTransactionRequest;
import com.schwab.eventledger.common.EventType;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
class TransactionRepository {
    private final JdbcTemplate jdbcTemplate;

    TransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    boolean apply(String accountId, AccountTransactionRequest request) {
        try {
            jdbcTemplate.update("""
                    insert into transactions(event_id, account_id, type, amount, currency, event_timestamp, applied_at)
                    values (?, ?, ?, ?, ?, ?, ?)
                    """,
                    request.eventId(),
                    accountId,
                    request.type().name(),
                    request.amount(),
                    request.currency(),
                    Timestamp.from(request.eventTimestamp()),
                    Timestamp.from(Instant.now()));
            return true;
        } catch (DuplicateKeyException duplicate) {
            return false;
        }
    }

    BigDecimal balance(String accountId) {
        var value = jdbcTemplate.queryForObject("""
                select coalesce(sum(case when type = 'CREDIT' then amount else -amount end), 0)
                from transactions
                where account_id = ?
                """, BigDecimal.class, accountId);
        return value == null ? BigDecimal.ZERO : value;
    }

    Optional<String> currency(String accountId) {
        return jdbcTemplate.query("""
                select currency from transactions where account_id = ? order by applied_at desc limit 1
                """, (rs, rowNum) -> rs.getString("currency"), accountId).stream().findFirst();
    }

    List<AccountDetailsResponse.TransactionSummary> recentTransactions(String accountId) {
        return jdbcTemplate.query("""
                select event_id, type, amount, currency, event_timestamp
                from transactions
                where account_id = ?
                order by event_timestamp desc, event_id desc
                limit 10
                """, this::summary, accountId);
    }

    int countRows() {
        Integer count = jdbcTemplate.queryForObject("select count(*) from transactions", Integer.class);
        return count == null ? 0 : count;
    }

    private AccountDetailsResponse.TransactionSummary summary(ResultSet rs, int rowNum) throws SQLException {
        return new AccountDetailsResponse.TransactionSummary(
                rs.getString("event_id"),
                EventType.valueOf(rs.getString("type")),
                rs.getBigDecimal("amount"),
                rs.getString("currency"),
                rs.getTimestamp("event_timestamp").toInstant()
        );
    }
}
