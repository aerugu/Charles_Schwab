package com.schwab.eventledger.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Configuration for outbound Gateway calls to the Account Service.
 *
 * @param baseUrl Account Service base URL
 * @param timeoutMs connect and read timeout in milliseconds
 * @param maxAttempts maximum attempts per operation, including the first try
 * @param backoffMs base retry delay before exponential multiplication
 * @param jitterMs maximum random jitter added to retry delay
 * @param circuitFailureThreshold consecutive failures before opening the circuit
 * @param circuitOpenMs duration to fail fast before allowing a trial call
 */
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
