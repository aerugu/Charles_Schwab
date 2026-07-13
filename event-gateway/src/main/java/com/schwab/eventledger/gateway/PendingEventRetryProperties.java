package com.schwab.eventledger.gateway;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

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
