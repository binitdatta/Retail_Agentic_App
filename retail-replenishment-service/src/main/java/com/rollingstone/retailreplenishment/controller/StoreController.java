package com.rollingstone.retailreplenishment.controller;

import com.rollingstone.retailreplenishment.dto.StoreDto;
import com.rollingstone.retailreplenishment.service.StoreService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public List<StoreDto> findAll() {
        return storeService.findAll();
    }

    @GetMapping("/{id}")
    public StoreDto findById(@PathVariable Long id) {
        return storeService.findById(id);
    }
}
