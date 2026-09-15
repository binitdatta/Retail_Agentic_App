package com.rollingstone.retailreplenishment.controller;

import com.rollingstone.retailreplenishment.dto.CreateEscalationRequest;
import com.rollingstone.retailreplenishment.dto.EscalationDto;
import com.rollingstone.retailreplenishment.dto.UpdateEscalationStatusRequest;
import com.rollingstone.retailreplenishment.service.EscalationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/escalations")
public class EscalationController {

    private final EscalationService escalationService;

    public EscalationController(EscalationService escalationService) {
        this.escalationService = escalationService;
    }

    // Backs the "escalate shortage" stage.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EscalationDto create(@Valid @RequestBody CreateEscalationRequest request) {
        return escalationService.create(request);
    }

    @GetMapping
    public List<EscalationDto> search(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String statusCode) {
        return escalationService.search(storeId, statusCode);
    }

    @PatchMapping("/{id}/status")
    public EscalationDto updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateEscalationStatusRequest request) {
        return escalationService.updateStatus(id, request);
    }
}
