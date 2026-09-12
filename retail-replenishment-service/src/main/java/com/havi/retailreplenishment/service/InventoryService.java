package com.havi.retailreplenishment.service;

import com.havi.retailreplenishment.dto.LowStockItemDto;
import com.havi.retailreplenishment.mapper.StoreInventoryMapper;
import com.havi.retailreplenishment.repository.StoreInventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Backs the "detect low inventory" stage. findLowStock mirrors
 * vw_low_stock_inventory from the DDL — kept as one reviewable JPQL query
 * rather than duplicated across callers.
 */
@Service
@Transactional(readOnly = true)
public class InventoryService {

    private final StoreInventoryRepository storeInventoryRepository;

    public InventoryService(StoreInventoryRepository storeInventoryRepository) {
        this.storeInventoryRepository = storeInventoryRepository;
    }

    public List<LowStockItemDto> findLowStock(Long storeId) {
        return storeInventoryRepository.findLowStock(storeId).stream()
            .map(StoreInventoryMapper::toLowStockDto)
            .toList();
    }
}
