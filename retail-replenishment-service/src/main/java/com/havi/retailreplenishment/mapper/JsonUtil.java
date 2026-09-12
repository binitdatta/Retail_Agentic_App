package com.havi.retailreplenishment.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Centralizes conversion between the raw JSON strings stored in the
 * json-typed columns (agent_decision_log, llm_call_log) and the structured
 * Object payloads used at the DTO boundary, so request/response bodies carry
 * real nested JSON instead of an escaped string.
 */
@Component
public class JsonUtil {

    private final ObjectMapper objectMapper;

    public JsonUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to serialize payload to JSON", e);
        }
    }

    public Object fromJson(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to parse stored JSON payload", e);
        }
    }
}
