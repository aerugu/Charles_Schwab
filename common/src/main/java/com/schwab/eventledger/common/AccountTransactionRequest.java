package com.schwab.eventledger.common;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Internal request sent from the Event Gateway to the Account Service to apply
 * one financial transaction. The {@code eventId} is the idempotency key for the
 * Account Service.
 *
 * @param eventId unique event identifier
 * @param type credit or debit transaction type
 * @param amount positive transaction amount
 * @param currency ISO-style currency code, such as USD
 * @param eventTimestamp timestamp when the upstream event originally occurred
 */
public record AccountTransactionRequest(
        @NotBlank String eventId,
        @NotNull EventType type,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
        @NotBlank String currency,
        @NotNull Instant eventTimestamp
) {
}
