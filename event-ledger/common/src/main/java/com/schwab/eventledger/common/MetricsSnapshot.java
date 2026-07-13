package com.schwab.eventledger.common;

import java.util.Map;

/**
 * Simple metrics snapshot exposed by both services.
 *
 * @param service logical service name
 * @param requestCounts request totals keyed by endpoint template
 * @param errorCounts error response totals keyed by endpoint template
 */
public record MetricsSnapshot(String service, Map<String, Long> requestCounts, Map<String, Long> errorCounts) {
}
