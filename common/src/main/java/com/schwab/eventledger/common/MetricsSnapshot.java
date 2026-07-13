package com.schwab.eventledger.common;

import java.util.Map;

public record MetricsSnapshot(String service, Map<String, Long> requestCounts, Map<String, Long> errorCounts) {
}
