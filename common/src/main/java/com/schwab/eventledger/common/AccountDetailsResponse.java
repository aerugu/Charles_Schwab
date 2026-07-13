package com.schwab.eventledger.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AccountDetailsResponse(
        String accountId,
        BigDecimal balance,
        String currency,
        List<TransactionSummary> recentTransactions
) {
    public record TransactionSummary(
            String eventId,
            EventType type,
            BigDecimal amount,
            String currency,
            Instant eventTimestamp
    ) {
    }
}
