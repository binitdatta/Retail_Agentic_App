package com.rollingstone.retailreplenishment.controller;

import com.rollingstone.retailreplenishment.dto.*;
import com.rollingstone.retailreplenishment.dto.*;
import com.rollingstone.retailreplenishment.service.DeliveryService;
import com.rollingstone.retailreplenishment.service.ReplenishmentOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/replenishment-orders")
public class ReplenishmentOrderController {

    private final ReplenishmentOrderService orderService;
    private final DeliveryService deliveryService;

    public ReplenishmentOrderController(ReplenishmentOrderService orderService, DeliveryService deliveryService) {
        this.orderService = orderService;
        this.deliveryService = deliveryService;
    }

    // Backs the "recommend/create replenishment order" stage.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReplenishmentOrderDto create(@Valid @RequestBody CreateReplenishmentOrderRequest request) {
        return orderService.createOrder(request);
    }

    @GetMapping("/{id}")
    public ReplenishmentOrderDto getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @GetMapping
    public List<ReplenishmentOrderDto> search(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String statusCode) {
        return orderService.search(storeId, statusCode);
    }

    @PatchMapping("/{id}/status")
    public ReplenishmentOrderDto updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateStatus(id, request);
    }

    // Backs the "monitor delivery" stage's read side.
    @GetMapping("/{id}/delivery-status")
    public List<DeliveryStatusDto> deliveryStatus(@PathVariable Long id) {
        return deliveryService.findByOrderId(id);
    }

    @PostMapping("/{id}/shipments")
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryStatusDto createShipment(@PathVariable Long id, @RequestBody CreateShipmentRequest request) {
        return deliveryService.createShipment(id, request);
    }
}
