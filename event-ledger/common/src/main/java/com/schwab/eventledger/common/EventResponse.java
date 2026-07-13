package com.schwab.eventledger.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Event Ledger representation returned by the Gateway.
 *
 * @param eventId unique event identifier and Gateway idempotency key
 * @param accountId account associated with the event
 * @param type credit or debit event type
 * @param amount positive event amount
 * @param currency event currency
 * @param eventTimestamp timestamp when the upstream event originally occurred
 * @param metadata optional upstream metadata preserved by the Gateway
 * @param receivedAt timestamp when the Gateway first stored the event
 * @param duplicate true when the response is for a repeated submission
 */
public record EventResponse(
        String eventId,
        String accountId,
        EventType type,
        BigDecimal amount,
        String currency,
        Instant eventTimestamp,
        Map<String, Object> metadata,
        Instant receivedAt,
        boolean duplicate
) {
}
