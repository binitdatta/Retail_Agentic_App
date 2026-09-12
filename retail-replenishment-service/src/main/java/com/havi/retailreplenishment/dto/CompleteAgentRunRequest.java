package com.havi.retailreplenishment.dto;

import jakarta.validation.constraints.NotBlank;

public record CompleteAgentRunRequest(
    @NotBlank String status,
    String summary
) {}
