package com.schwab.eventledger.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schwab.eventledger.common.EventType;
import com.schwab.eventledger.common.TransactionEventRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
class EventRepository {
    private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    EventRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    EventRecord save(TransactionEventRequest request) {
        var receivedAt = Instant.now();
        jdbcTemplate.update("""
                insert into events(event_id, account_id, type, amount, currency, event_timestamp, metadata_json, received_at)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                request.eventId(),
                request.accountId(),
                request.type().name(),
                request.amount(),
                request.currency(),
                Timestamp.from(request.eventTimestamp()),
                writeMetadata(request.metadata()),
                Timestamp.from(receivedAt));
        return new EventRecord(
                request.eventId(),
                request.accountId(),
                request.type(),
                request.amount(),
                request.currency(),
                request.eventTimestamp(),
                request.metadata() == null ? Map.of() : request.metadata(),
                receivedAt
        );
    }

    SaveAttempt saveOrFindExisting(TransactionEventRequest request) {
        try {
            return new SaveAttempt(save(request), true);
        } catch (DuplicateKeyException duplicate) {
            return new SaveAttempt(findById(request.eventId()).orElseThrow(() -> duplicate), false);
        }
    }

    void deleteById(String eventId) {
        jdbcTemplate.update("delete from events where event_id = ?", eventId);
    }

    record SaveAttempt(EventRecord record, boolean created) {
    }

    Optional<EventRecord> findById(String eventId) {
        return jdbcTemplate.query("""
                select event_id, account_id, type, amount, currency, event_timestamp, metadata_json, received_at
                from events
                where event_id = ?
                """, this::mapRow, eventId).stream().findFirst();
    }

    List<EventRecord> findByAccount(String accountId) {
        return jdbcTemplate.query("""
                select event_id, account_id, type, amount, currency, event_timestamp, metadata_json, received_at
                from events
                where account_id = ?
                order by event_timestamp asc, event_id asc
                """, this::mapRow, accountId);
    }

    int countRows() {
        Integer count = jdbcTemplate.queryForObject("select count(*) from events", Integer.class);
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

    private EventRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new EventRecord(
                rs.getString("event_id"),
                rs.getString("account_id"),
                EventType.valueOf(rs.getString("type")),
                rs.getBigDecimal("amount"),
                rs.getString("currency"),
                rs.getTimestamp("event_timestamp").toInstant(),
                readMetadata(rs.getString("metadata_json")),
                rs.getTimestamp("received_at").toInstant()
        );
    }

    private String writeMetadata(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("metadata must be JSON serializable", e);
        }
    }

    private Map<String, Object> readMetadata(String metadataJson) {
        try {
            return objectMapper.readValue(metadataJson == null ? "{}" : metadataJson, METADATA_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("stored metadata is not readable", e);
        }
    }
}
