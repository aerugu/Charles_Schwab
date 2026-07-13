package com.schwab.eventledger.gateway;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Token-bucket rate limit configuration for the Gateway.
 *
 * @param enabled whether rate limiting is active
 * @param capacity maximum burst size
 * @param refillPerSecond number of tokens replenished per second
 */
@Validated
@ConfigurationProperties(prefix = "gateway.rate-limit")
record RateLimitProperties(
        boolean enabled,
        @Min(1)
        int capacity,
        @Min(1)
        int refillPerSecond
) {
}
