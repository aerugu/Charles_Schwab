package com.schwab.eventledger.common;

/**
 * Supported financial transaction directions.
 */
public enum EventType {
    /**
     * Adds the event amount to the account balance.
     */
    CREDIT,
    /**
     * Subtracts the event amount from the account balance.
     */
    DEBIT
}
