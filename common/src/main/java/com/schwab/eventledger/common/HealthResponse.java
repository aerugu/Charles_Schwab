package com.schwab.eventledger.common;

import java.time.Instant;
import java.util.Map;

/**
 * Service health response with lightweight diagnostics.
 *
 * @param service logical service name
 * @param status overall service status, typically UP or DOWN
 * @param timestamp server-side time when health was evaluated
 * @param diagnostics service-specific diagnostic values
 */
public record HealthResponse(String service, String status, Instant timestamp, Map<String, Object> diagnostics) {
}
