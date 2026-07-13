package com.schwab.eventledger.common;

import java.time.Instant;
import java.util.Map;

public record HealthResponse(String service, String status, Instant timestamp, Map<String, Object> diagnostics) {
}
