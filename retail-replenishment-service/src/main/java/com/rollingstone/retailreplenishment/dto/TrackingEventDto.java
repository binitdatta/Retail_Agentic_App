package com.rollingstone.retailreplenishment.dto;

import java.time.LocalDateTime;

public record TrackingEventDto(
    String eventCode,
    LocalDateTime eventAt,
    String eventLocation,
    String notes
) {}
