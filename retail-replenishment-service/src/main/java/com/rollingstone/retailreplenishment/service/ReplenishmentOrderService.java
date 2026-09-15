package com.rollingstone.retailreplenishment.service;

import com.rollingstone.retailreplenishment.dto.*;
import com.rollingstone.retailreplenishment.entity.*;
import com.rollingstone.retailreplenishment.dto.CreateReplenishmentOrderRequest;
import com.rollingstone.retailreplenishment.dto.OrderLineRequest;
import com.rollingstone.retailreplenishment.dto.ReplenishmentOrderDto;
import com.rollingstone.retailreplenishment.dto.UpdateOrderStatusRequest;
import com.rollingstone.retailreplenishment.entity.*;
import com.rollingstone.retailreplenishment.exception.InvalidStateTransitionException;
import com.rollingstone.retailreplenishment.exception.ResourceNotFoundException;
import com.rollingstone.retailreplenishment.mapper.ReplenishmentOrderMapper;
import com.rollingstone.retailreplenishment.messaging.EventPublisher;
import com.rollingstone.retailreplenishment.repository.*;
import com.rollingstone.retailreplenishment.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Backs the "recommend/create replenishment order" stage, plus the
 * human/agent approval and supplier-transmission transitions that follow it.
 */
@Service
@Transactional
public class ReplenishmentOrderService {

    private final ReplenishmentOrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final DemandForecastRunRepository forecastRunRepository;
    private final RefOrderStatusRepository orderStatusRepository;
    private final AgentRunRepository agentRunRepository;
    private final EventPublisher eventPublisher;

    public ReplenishmentOrderService(
            ReplenishmentOrderRepository orderRepository,
            StoreRepository storeRepository,
            SupplierRepository supplierRepository,
            ProductRepository productRepository,
            DemandForecastRunRepository forecastRunRepository,
            RefOrderStatusRepository orderStatusRepository,
            AgentRunRepository agentRunRepository,
            EventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.storeRepository = storeRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.forecastRunRepository = forecastRunRepository;
        this.orderStatusRepository = orderStatusRepository;
        this.agentRunRepository = agentRunRepository;
        this.eventPublisher = eventPublisher;
    }

    public ReplenishmentOrderDto createOrder(CreateReplenishmentOrderRequest request) {
        Store store = storeRepository.findById(request.storeId())
            .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + request.storeId()));
        Supplier supplier = supplierRepository.findById(request.supplierId())
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + request.supplierId()));

        boolean autoApproved = request.approvedBy() != null && !request.approvedBy().isBlank();
        String initialStatusCode = autoApproved ? "APPROVED" : "RECOMMENDED";
        RefOrderStatus status = orderStatusRepository.findById(initialStatusCode)
            .orElseThrow(() -> new IllegalStateException("Reference status missing: " + initialStatusCode));

        ReplenishmentOrder order = new ReplenishmentOrder();
        order.setStore(store);
        order.setSupplier(supplier);
        order.setStatus(status);
        order.setSourceType(request.sourceType());
        order.setRequestedDeliveryDate(request.requestedDeliveryDate());
        if (autoApproved) {
            order.setApprovedBy(request.approvedBy());
            order.setApprovedAt(LocalDateTime.now());
        }
        if (request.generatedByAgentRunId() != null) {
            AgentRun agentRun = agentRunRepository.findById(request.generatedByAgentRunId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent run not found: " + request.generatedByAgentRunId()));
            order.setGeneratedByAgentRun(agentRun);
        }

        BigDecimal totalCost = BigDecimal.ZERO;
        for (OrderLineRequest lineReq : request.lines()) {
            Product product = productRepository.findById(lineReq.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + lineReq.productId()));

            ReplenishmentOrderLine line = new ReplenishmentOrderLine();
            line.setProduct(product);
            line.setOrderedQty(lineReq.orderedQty());
            line.setUnitCost(lineReq.unitCost());
            if (lineReq.forecastRunId() != null) {
                DemandForecastRun forecastRun = forecastRunRepository.findById(lineReq.forecastRunId())
                    .orElseThrow(() -> new ResourceNotFoundException("Forecast run not found: " + lineReq.forecastRunId()));
                line.setForecastRun(forecastRun);
            }
            order.addLine(line);
            totalCost = totalCost.add(lineReq.orderedQty().multiply(lineReq.unitCost()));
        }
        order.setTotalCost(totalCost);
        order.setOrderNumber(generateUniqueOrderNumber());

        ReplenishmentOrder saved = saveWithOrderNumberRetry(order);

        eventPublisher.publishOrderCreated(
            saved.getReplenishmentOrderId(), saved.getOrderNumber(),
            store.getStoreId(), supplier.getSupplierId());

        return ReplenishmentOrderMapper.toDto(fetchWithLines(saved.getReplenishmentOrderId()));
    }

    private ReplenishmentOrder saveWithOrderNumberRetry(ReplenishmentOrder order) {
        int attempts = 0;
        while (true) {
            try {
                return orderRepository.saveAndFlush(order);
            } catch (DataIntegrityViolationException e) {
                if (++attempts >= 3) throw e;
                order.setOrderNumber(generateUniqueOrderNumber());
            }
        }
    }

    private String generateUniqueOrderNumber() {
        int year = Year.now().getValue();
        int suffix = ThreadLocalRandom.current().nextInt(100_000);
        return String.format("RO-%d-%05d", year, suffix);
    }

    public ReplenishmentOrderDto getById(Long id) {
        return ReplenishmentOrderMapper.toDto(fetchWithLines(id));
    }

    public List<ReplenishmentOrderDto> search(Long storeId, String statusCode) {
        return orderRepository.search(storeId, statusCode).stream()
            .map(ReplenishmentOrderMapper::toDto)
            .toList();
    }

    public ReplenishmentOrderDto updateStatus(Long id, UpdateOrderStatusRequest request) {
        ReplenishmentOrder order = fetchWithLines(id);
        if (Boolean.TRUE.equals(order.getStatus().getTerminal())) {
            throw new InvalidStateTransitionException(
                "Order " + order.getOrderNumber() + " is already in a terminal state: " + order.getStatus().getStatusCode());
        }
        RefOrderStatus newStatus = orderStatusRepository.findById(request.statusCode())
            .orElseThrow(() -> new IllegalArgumentException("Unknown order status: " + request.statusCode()));

        order.setStatus(newStatus);
        LocalDateTime now = LocalDateTime.now();
        switch (request.statusCode()) {
            case "APPROVED" -> {
                order.setApprovedAt(now);
                order.setApprovedBy(request.actorName());
            }
            case "SENT_TO_SUPPLIER" -> order.setSentToSupplierAt(now);
            default -> { /* no timestamp side-effect for other transitions */ }
        }
        ReplenishmentOrder saved = orderRepository.save(order);

        if ("APPROVED".equals(request.statusCode())) {
            // A RECOMMENDED order didn't get shipped when it was first created
            // (see SupplierIntegrationStubListener) — now that a human has
            // approved it, give the stub a second chance to act on the same
            // event.
            eventPublisher.publishOrderCreated(
                saved.getReplenishmentOrderId(), saved.getOrderNumber(),
                saved.getStore().getStoreId(), saved.getSupplier().getSupplierId());
        }

        return ReplenishmentOrderMapper.toDto(saved);
    }

    private ReplenishmentOrder fetchWithLines(Long id) {
        return orderRepository.findByIdWithLines(id)
            .orElseThrow(() -> new ResourceNotFoundException("Replenishment order not found: " + id));
    }
}
