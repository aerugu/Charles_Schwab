package com.schwab.eventledger.account;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
class JsonLogger {
    private final ObjectMapper objectMapper;

    JsonLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void info(String message, Map<String, Object> fields) {
        write("INFO", message, fields);
    }

    void warn(String message, Map<String, Object> fields) {
        write("WARN", message, fields);
    }

    private void write(String level, String message, Map<String, Object> fields) {
        var entry = new LinkedHashMap<String, Object>();
        entry.put("timestamp", Instant.now().toString());
        entry.put("level", level);
        entry.put("service", "account-service");
        entry.put("traceId", TraceContext.get());
        entry.put("message", message);
        entry.putAll(fields);
        try {
            System.out.println(objectMapper.writeValueAsString(entry));
        } catch (JsonProcessingException e) {
            System.out.println(entry);
        }
    }
}
