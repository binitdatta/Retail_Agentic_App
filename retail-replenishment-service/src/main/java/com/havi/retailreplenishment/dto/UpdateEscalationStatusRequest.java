package com.havi.retailreplenishment.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateEscalationStatusRequest(
    @NotBlank String statusCode,
    String assignedTo,
    String resolutionNotes
) {}
