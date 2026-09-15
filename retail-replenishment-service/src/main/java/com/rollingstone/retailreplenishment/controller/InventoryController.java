package com.rollingstone.retailreplenishment.controller;

import com.rollingstone.retailreplenishment.dto.LowStockItemDto;
import com.rollingstone.retailreplenishment.service.InventoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    // GET /api/inventory/low-stock or /api/inventory/low-stock?storeId=1
    // Backs the "detect low inventory" stage.
    @GetMapping("/low-stock")
    public List<LowStockItemDto> lowStock(@RequestParam(required = false) Long storeId) {
        return inventoryService.findLowStock(storeId);
    }
}
