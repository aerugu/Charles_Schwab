package com.schwab.eventledger.account;

/**
 * Request-scoped trace ID holder used by Account Service structured logging.
 */
final class TraceContext {
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    private TraceContext() {
    }

    static void set(String traceId) {
        TRACE_ID.set(traceId);
    }

    static String get() {
        return TRACE_ID.get();
    }

    static void clear() {
        TRACE_ID.remove();
    }
}
