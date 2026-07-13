package com.schwab.eventledger.common;

import java.math.BigDecimal;

/**
 * Current balance for an account.
 *
 * @param accountId account identifier
 * @param balance net balance, computed as total credits minus total debits
 * @param currency balance currency
 */
public record BalanceResponse(String accountId, BigDecimal balance, String currency) {
}
