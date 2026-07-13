package com.schwab.eventledger.common;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Public event submission payload accepted by the Event Gateway.
 *
 * @param eventId unique event identifier and Gateway idempotency key
 * @param accountId account that should receive the transaction
 * @param type credit or debit event type
 * @param amount positive event amount
 * @param currency ISO-style currency code, such as USD
 * @param eventTimestamp timestamp when the upstream event originally occurred
 * @param metadata optional source metadata preserved in the ledger
 */
public record TransactionEventRequest(
        @NotBlank(message = "is required") String eventId,
        @NotBlank(message = "is required") String accountId,
        @NotNull(message = "is required and must be CREDIT or DEBIT") EventType type,
        @NotNull(message = "is required") @DecimalMin(value = "0.0", inclusive = false, message = "must be greater than 0") BigDecimal amount,
        @NotBlank(message = "is required") String currency,
        @NotNull(message = "is required and must be ISO-8601") Instant eventTimestamp,
        Map<String, Object> metadata
) {
}
