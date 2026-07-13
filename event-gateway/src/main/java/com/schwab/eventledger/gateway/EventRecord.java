package com.schwab.eventledger.gateway;

import com.schwab.eventledger.common.EventResponse;
import com.schwab.eventledger.common.EventType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

record EventRecord(
        String eventId,
        String accountId,
        EventType type,
        BigDecimal amount,
        String currency,
        Instant eventTimestamp,
        Map<String, Object> metadata,
        Instant receivedAt
) {
    EventResponse toResponse(boolean duplicate) {
        return new EventResponse(eventId, accountId, type, amount, currency, eventTimestamp, metadata, receivedAt, duplicate);
    }
}
