package com.havi.retailreplenishment.dto;

public record StoreDto(
    Long storeId,
    String storeCode,
    String storeName,
    String region,
    String city,
    String stateProvince,
    Boolean active
) {}
