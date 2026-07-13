package com.schwab.eventledger.gateway;

import java.time.Instant;

/**
 * Public representation of a Gateway audit entry.
 *
 * <p>Audit entries capture significant lifecycle decisions for a submitted
 * event, including duplicate handling and downstream Account Service outcomes.
 * The record is intentionally immutable so responses reflect the stored audit
 * trail without controller-side mutation.</p>
 */
public record AuditEntryResponse(
        long id,
        String eventId,
        String accountId,
        String action,
        String traceId,
        String detail,
        Instant createdAt
) {
}
