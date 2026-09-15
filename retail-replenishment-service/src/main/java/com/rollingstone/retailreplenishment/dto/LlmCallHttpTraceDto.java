package com.rollingstone.retailreplenishment.dto;

import java.time.LocalDateTime;

public record LlmCallHttpTraceDto(
    Long httpTraceId,
    Long llmCallId,
    String providerCode,
    String modelName,
    String httpMethod,
    String requestUrl,
    Object requestHeaders,
    Object requestParams,
    Object requestBody,
    Integer responseStatusCode,
    Object responseHeaders,
    Object responseBody,
    LocalDateTime createdAt
) {}
