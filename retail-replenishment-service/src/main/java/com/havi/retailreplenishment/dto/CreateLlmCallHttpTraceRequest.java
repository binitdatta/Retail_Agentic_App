package com.havi.retailreplenishment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateLlmCallHttpTraceRequest(
    @NotBlank String httpMethod,
    @NotBlank String requestUrl,
    @NotNull Object requestHeaders,
    Object requestParams,
    @NotNull Object requestBody,
    Integer responseStatusCode,
    Object responseHeaders,
    Object responseBody
) {}
