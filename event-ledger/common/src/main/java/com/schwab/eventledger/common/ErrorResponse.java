package com.schwab.eventledger.common;

import java.time.Instant;
import java.util.List;

/**
 * Standard error payload returned by both services.
 *
 * @param timestamp server-side time when the error response was created
 * @param status HTTP status code
 * @param error high-level error category
 * @param messages detailed validation or failure messages
 * @param traceId trace identifier associated with the failed request
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        List<String> messages,
        String traceId
) {
}
