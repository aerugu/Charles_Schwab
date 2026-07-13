package com.schwab.eventledger.gateway;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Gateway-owned audit repository for event lifecycle decisions.
 *
 * <p>The audit store is kept in the Gateway database because it records public
 * API decisions made at the ingestion boundary. Account Service transaction
 * state remains owned by the Account Service database.</p>
 */
@Repository
class AuditRepository {
    private final JdbcTemplate jdbcTemplate;

    AuditRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void record(String eventId, String accountId, AuditAction action, String detail) {
        jdbcTemplate.update("""
                insert into audit_entries(event_id, account_id, action, trace_id, detail, created_at)
                values (?, ?, ?, ?, ?, ?)
                """,
                eventId,
                accountId,
                action.name(),
                traceId(),
                trim(detail),
                Timestamp.from(Instant.now()));
    }

    List<AuditEntryResponse> findByEventId(String eventId) {
        return jdbcTemplate.query("""
                select id, event_id, account_id, action, trace_id, detail, created_at
                from audit_entries
                where event_id = ?
                order by id asc
                """, this::mapRow, eventId);
    }

    int countRows() {
        Integer count = jdbcTemplate.queryForObject("select count(*) from audit_entries", Integer.class);
        return count == null ? 0 : count;
    }

    boolean databaseAvailable() {
        try {
            Integer result = jdbcTemplate.queryForObject("select 1", Integer.class);
            return result != null && result == 1;
        } catch (DataAccessException ex) {
            return false;
        }
    }

    private AuditEntryResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new AuditEntryResponse(
                rs.getLong("id"),
                rs.getString("event_id"),
                rs.getString("account_id"),
                rs.getString("action"),
                rs.getString("trace_id"),
                rs.getString("detail"),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private String trim(String detail) {
        if (detail == null) {
            return null;
        }
        return detail.length() > 512 ? detail.substring(0, 512) : detail;
    }

    private String traceId() {
        var traceId = TraceContext.get();
        return traceId == null || traceId.isBlank() ? "missing-trace-id" : traceId;
    }

    enum AuditAction {
        EVENT_ACCEPTED,
        DUPLICATE_SUBMISSION,
        ACCOUNT_APPLY_SUCCEEDED,
        EVENT_QUEUED_FOR_RETRY
    }
}
