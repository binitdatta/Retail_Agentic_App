package com.havi.retailreplenishment.service;

import com.havi.retailreplenishment.dto.*;
import com.havi.retailreplenishment.entity.*;
import com.havi.retailreplenishment.exception.ResourceNotFoundException;
import com.havi.retailreplenishment.mapper.EscalationMapper;
import com.havi.retailreplenishment.messaging.EventPublisher;
import com.havi.retailreplenishment.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Backs the "escalate shortage" stage. The reason/severity value sets are
 * validated here so a bad value comes back as a clean 400 instead of a raw
 * CHECK-constraint SQL error surfacing from the DB.
 */
@Service
@Transactional
public class EscalationService {

    private static final Set<String> VALID_REASONS = Set.of(
        "SUPPLIER_UNAVAILABLE", "DELIVERY_DELAYED", "DEMAND_SPIKE", "CAPACITY_CONSTRAINT", "OTHER");
    private static final Set<String> VALID_SEVERITIES = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    private final ShortageEscalationRepository escalationRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final ReplenishmentOrderRepository orderRepository;
    private final AgentRunRepository agentRunRepository;
    private final RefEscalationStatusRepository escalationStatusRepository;
    private final EventPublisher eventPublisher;

    public EscalationService(
            ShortageEscalationRepository escalationRepository,
            StoreRepository storeRepository,
            ProductRepository productRepository,
            ReplenishmentOrderRepository orderRepository,
            AgentRunRepository agentRunRepository,
            RefEscalationStatusRepository escalationStatusRepository,
            EventPublisher eventPublisher) {
        this.escalationRepository = escalationRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.agentRunRepository = agentRunRepository;
        this.escalationStatusRepository = escalationStatusRepository;
        this.eventPublisher = eventPublisher;
    }

    public EscalationDto create(CreateEscalationRequest request) {
        if (!VALID_REASONS.contains(request.escalationReason())) {
            throw new IllegalArgumentException("Unknown escalation reason: " + request.escalationReason());
        }
        if (!VALID_SEVERITIES.contains(request.severity())) {
            throw new IllegalArgumentException("Unknown severity: " + request.severity());
        }

        Store store = storeRepository.findById(request.storeId())
            .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + request.storeId()));
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.productId()));
        RefEscalationStatus open = escalationStatusRepository.findById("OPEN")
            .orElseThrow(() -> new IllegalStateException("Reference status missing: OPEN"));

        ShortageEscalation escalation = new ShortageEscalation();
        escalation.setStore(store);
        escalation.setProduct(product);
        escalation.setEscalationReason(request.escalationReason());
        escalation.setSeverity(request.severity());
        escalation.setStatus(open);

        if (request.replenishmentOrderId() != null) {
            ReplenishmentOrder order = orderRepository.findById(request.replenishmentOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Replenishment order not found: " + request.replenishmentOrderId()));
            escalation.setReplenishmentOrder(order);
        }
        if (request.raisedByAgentRunId() != null) {
            AgentRun agentRun = agentRunRepository.findById(request.raisedByAgentRunId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent run not found: " + request.raisedByAgentRunId()));
            escalation.setRaisedByAgentRun(agentRun);
        }

        ShortageEscalation saved = escalationRepository.save(escalation);
        eventPublisher.publishShortageEscalated(
            saved.getEscalationId(), store.getStoreId(), product.getProductId(), request.severity());
        return EscalationMapper.toDto(saved);
    }

    public EscalationDto updateStatus(Long id, UpdateEscalationStatusRequest request) {
        ShortageEscalation escalation = escalationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Escalation not found: " + id));
        RefEscalationStatus newStatus = escalationStatusRepository.findById(request.statusCode())
            .orElseThrow(() -> new IllegalArgumentException("Unknown escalation status: " + request.statusCode()));

        escalation.setStatus(newStatus);
        if (request.assignedTo() != null) escalation.setAssignedTo(request.assignedTo());
        if (request.resolutionNotes() != null) escalation.setResolutionNotes(request.resolutionNotes());
        if (Boolean.TRUE.equals(newStatus.getTerminal())) {
            escalation.setResolvedAt(LocalDateTime.now());
        }
        return EscalationMapper.toDto(escalationRepository.save(escalation));
    }

    public List<EscalationDto> search(Long storeId, String statusCode) {
        return escalationRepository.search(storeId, statusCode).stream()
            .map(EscalationMapper::toDto)
            .toList();
    }
}
