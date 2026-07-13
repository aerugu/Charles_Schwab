package com.schwab.eventledger.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "account-service")
record AccountServiceProperties(
        @NotBlank
        String baseUrl,
        @Min(1)
        int timeoutMs,
        @Min(1)
        int maxAttempts,
        @Min(0)
        long backoffMs,
        @Min(0)
        long jitterMs,
        @Min(1)
        int circuitFailureThreshold,
        @Min(1)
        long circuitOpenMs
) {
}
