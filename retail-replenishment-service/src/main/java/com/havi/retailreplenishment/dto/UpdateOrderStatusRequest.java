package com.havi.retailreplenishment.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateOrderStatusRequest(
    @NotBlank String statusCode,
    String actorName
) {}
