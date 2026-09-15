package com.rollingstone.retailreplenishment.controller;

import com.rollingstone.retailreplenishment.dto.DeliveryStatusDto;
import com.rollingstone.retailreplenishment.dto.UpdateShipmentStatusRequest;
import com.rollingstone.retailreplenishment.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final DeliveryService deliveryService;

    public ShipmentController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    // A status update to DELIVERED also receives stock into store_inventory
    // and closes the parent order — see DeliveryService for the detail.
    @PatchMapping("/{id}/status")
    public DeliveryStatusDto updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateShipmentStatusRequest request) {
        return deliveryService.updateShipmentStatus(id, request);
    }
}
