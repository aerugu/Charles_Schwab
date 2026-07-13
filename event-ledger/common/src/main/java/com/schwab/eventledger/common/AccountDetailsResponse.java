package com.schwab.eventledger.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Account summary returned by the Account Service and proxied by the Gateway.
 *
 * @param accountId account identifier owned by the Account Service
 * @param balance current computed balance for the account
 * @param currency currency associated with the account balance
 * @param recentTransactions most recent transactions applied to the account
 */
public record AccountDetailsResponse(
        String accountId,
        BigDecimal balance,
        String currency,
        List<TransactionSummary> recentTransactions
) {
    /**
     * Compact representation of an applied transaction in account details.
     *
     * @param eventId idempotency key of the source event
     * @param type transaction type, either credit or debit
     * @param amount positive transaction amount
     * @param currency transaction currency
     * @param eventTimestamp timestamp supplied by the upstream event source
     */
    public record TransactionSummary(
            String eventId,
            EventType type,
            BigDecimal amount,
            String currency,
            Instant eventTimestamp
    ) {
    }
}
