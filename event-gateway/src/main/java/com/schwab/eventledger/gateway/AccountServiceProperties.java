package com.schwab.eventledger.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "account-service")
record AccountServiceProperties(
        String baseUrl,
        int timeoutMs,
        int maxAttempts,
        long backoffMs,
        int circuitFailureThreshold,
        long circuitOpenMs
) {
}
