package com.rollingstone.retailreplenishment.controller;

import com.rollingstone.retailreplenishment.dto.LowStockItemDto;
import com.rollingstone.retailreplenishment.service.DemoResetService;
import com.rollingstone.retailreplenishment.service.InventoryService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * One endpoint, manager-only (see SecurityConfig — deliberately not
 * reachable by the agent's service account, this is a human operational
 * action). Also gated by DemoResetService's own enabled flag, so being
 * authorized to call this endpoint is necessary but not sufficient — the
 * environment itself must have opted in too.
 */
@RestController
@RequestMapping("/api/admin/demo-reset")
public class DemoResetController {

    private final DemoResetService demoResetService;
    private final InventoryService inventoryService;

    public DemoResetController(DemoResetService demoResetService, InventoryService inventoryService) {
        this.demoResetService = demoResetService;
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public List<LowStockItemDto> reset() {
        demoResetService.resetToDemoScenario();
        return inventoryService.findLowStock(null);
    }
}