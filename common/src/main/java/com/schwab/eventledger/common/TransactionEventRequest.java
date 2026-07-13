package com.schwab.eventledger.common;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

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
