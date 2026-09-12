package com.havi.retailreplenishment.controller;

import com.havi.retailreplenishment.dto.SupplierAvailabilityDto;
import com.havi.retailreplenishment.dto.SupplierDto;
import com.havi.retailreplenishment.service.SupplierAvailabilityService;
import com.havi.retailreplenishment.service.SupplierService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;
    private final SupplierAvailabilityService availabilityService;

    public SupplierController(SupplierService supplierService, SupplierAvailabilityService availabilityService) {
        this.supplierService = supplierService;
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public List<SupplierDto> findAll() {
        return supplierService.findAll();
    }

    @GetMapping("/{id}")
    public SupplierDto findById(@PathVariable Long id) {
        return supplierService.findById(id);
    }

    // GET /api/suppliers/availability?productId=42
    // Backs the "check supplier availability" stage.
    @GetMapping("/availability")
    public List<SupplierAvailabilityDto> availability(@RequestParam Long productId) {
        return availabilityService.findAvailability(productId);
    }
}
