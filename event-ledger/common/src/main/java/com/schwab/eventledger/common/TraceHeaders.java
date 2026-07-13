package com.schwab.eventledger.common;

/**
 * Shared HTTP header names used by both services.
 */
public final class TraceHeaders {
    /**
     * Correlation header propagated from Gateway to Account Service.
     */
    public static final String TRACE_ID = "X-Trace-Id";

    private TraceHeaders() {
    }
}
