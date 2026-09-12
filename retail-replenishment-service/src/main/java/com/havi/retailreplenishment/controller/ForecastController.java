package com.havi.retailreplenishment.controller;

import com.havi.retailreplenishment.dto.DemandForecastDto;
import com.havi.retailreplenishment.dto.ForecastRequest;
import com.havi.retailreplenishment.service.ForecastService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demand-forecasts")
public class ForecastController {

    private final ForecastService forecastService;

    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    // POST computes a fresh moving-average forecast and persists the run.
    // Backs the "forecast demand" stage.
    @PostMapping
    public DemandForecastDto compute(@Valid @RequestBody ForecastRequest request) {
        return forecastService.computeMovingAverageForecast(request);
    }

    // GET returns the most recently computed forecast without recomputing.
    @GetMapping
    public DemandForecastDto latest(@RequestParam Long storeId, @RequestParam Long productId) {
        return forecastService.latest(storeId, productId);
    }
}
