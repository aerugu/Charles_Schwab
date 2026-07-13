package com.schwab.eventledger.gateway;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the Gateway pending-event retry worker.
 *
 * @param enabled whether pending outbox processing is active
 * @param intervalMs scheduler delay and base retry delay in milliseconds
 * @param batchSize maximum number of due pending events processed per pass
 */
@Validated
@ConfigurationProperties(prefix = "gateway.pending-retry")
record PendingEventRetryProperties(
        boolean enabled,
        @Min(1)
        long intervalMs,
        @Min(1)
        int batchSize
) {
}
