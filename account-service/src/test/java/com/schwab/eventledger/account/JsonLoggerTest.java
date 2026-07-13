package com.schwab.eventledger.account;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonLoggerTest {
    private final PrintStream originalOut = System.out;

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        TraceContext.clear();
    }

    @Test
    void writesStructuredAccountServiceLogWithTraceId() throws Exception {
        var output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        TraceContext.set("trace-account-log-001");

        new JsonLogger(new ObjectMapper()).info("request_completed", Map.of("path", "/accounts/acct-123/balance"));

        var log = new ObjectMapper().readValue(
                output.toString(StandardCharsets.UTF_8),
                new TypeReference<Map<String, Object>>() {
                }
        );
        assertThat(log).containsEntry("service", "account-service");
        assertThat(log).containsEntry("traceId", "trace-account-log-001");
        assertThat(log).containsEntry("message", "request_completed");
    }
}
