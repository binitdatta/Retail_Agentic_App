package com.rollingstone.retailreplenishment.service;

import com.rollingstone.retailreplenishment.dto.*;
import com.rollingstone.retailreplenishment.entity.*;
import com.rollingstone.retailreplenishment.dto.CreateShipmentRequest;
import com.rollingstone.retailreplenishment.dto.DeliveryStatusDto;
import com.rollingstone.retailreplenishment.dto.UpdateShipmentStatusRequest;
import com.rollingstone.retailreplenishment.entity.*;
import com.rollingstone.retailreplenishment.exception.ResourceNotFoundException;
import com.rollingstone.retailreplenishment.mapper.DeliveryMapper;
import com.rollingstone.retailreplenishment.messaging.EventPublisher;
import com.rollingstone.retailreplenishment.repository.*;
import com.rollingstone.retailreplenishment.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Backs the "monitor delivery" stage. A status update to DELIVERED does two
 * things beyond recording the event: it receives the ordered quantities into
 * store_inventory and closes out the parent replenishment_order — so the
 * detect-low-inventory stage on the next agent run sees accurate stock
 * without a separate reconciliation step.
 */
@Service
@Transactional
public class DeliveryService {

    private static final Set<String> TRACKING_EVENT_CODES = Set.of(
        "DEPARTED", "IN_TRANSIT", "CUSTOMS", "OUT_FOR_DELIVERY", "DELIVERED", "DELAYED", "EXCEPTION");

    private final DeliveryShipmentRepository shipmentRepository;
    private final ReplenishmentOrderRepository orderRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final RefShipmentStatusRepository shipmentStatusRepository;
    private final RefOrderStatusRepository orderStatusRepository;
    private final EventPublisher eventPublisher;

    public DeliveryService(
            DeliveryShipmentRepository shipmentRepository,
            ReplenishmentOrderRepository orderRepository,
            StoreInventoryRepository storeInventoryRepository,
            RefShipmentStatusRepository shipmentStatusRepository,
            RefOrderStatusRepository orderStatusRepository,
            EventPublisher eventPublisher) {
        this.shipmentRepository = shipmentRepository;
        this.orderRepository = orderRepository;
        this.storeInventoryRepository = storeInventoryRepository;
        this.shipmentStatusRepository = shipmentStatusRepository;
        this.orderStatusRepository = orderStatusRepository;
        this.eventPublisher = eventPublisher;
    }

    public DeliveryStatusDto createShipment(Long orderId, CreateShipmentRequest request) {
        ReplenishmentOrder order = orderRepository.findByIdWithLines(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Replenishment order not found: " + orderId));

        RefShipmentStatus created = shipmentStatusRepository.findById("CREATED")
            .orElseThrow(() -> new IllegalStateException("Reference status missing: CREATED"));

        DeliveryShipment shipment = new DeliveryShipment();
        shipment.setReplenishmentOrder(order);
        shipment.setCarrierName(request.carrierName());
        shipment.setTrackingNumber(request.trackingNumber());
        shipment.setStatus(created);
        shipment.setEstimatedArrivalAt(request.estimatedArrivalAt());

        DeliveryShipment saved = shipmentRepository.save(shipment);
        eventPublisher.publishDeliveryStatusUpdated(saved.getShipmentId(), order.getReplenishmentOrderId(), "CREATED");
        return DeliveryMapper.toDto(saved);
    }

    public DeliveryStatusDto updateShipmentStatus(Long shipmentId, UpdateShipmentStatusRequest request) {
        DeliveryShipment shipment = shipmentRepository.findByIdWithOrder(shipmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + shipmentId));

        RefShipmentStatus newStatus = shipmentStatusRepository.findById(request.statusCode())
            .orElseThrow(() -> new IllegalArgumentException("Unknown shipment status: " + request.statusCode()));

        shipment.setStatus(newStatus);
        LocalDateTime now = LocalDateTime.now();

        if ("DEPARTED".equals(request.statusCode()) && shipment.getShippedAt() == null) {
            shipment.setShippedAt(now);
        }

        if (TRACKING_EVENT_CODES.contains(request.statusCode())) {
            DeliveryTrackingEvent event = new DeliveryTrackingEvent();
            event.setEventCode(request.statusCode());
            event.setEventAt(now);
            event.setEventLocation(request.eventLocation());
            event.setNotes(request.notes());
            shipment.addTrackingEvent(event);
        }

        if ("DELIVERED".equals(request.statusCode())) {
            shipment.setActualArrivalAt(request.actualArrivalAt() != null ? request.actualArrivalAt() : now);
            receiveIntoInventory(shipment.getReplenishmentOrder());
            closeOrder(shipment.getReplenishmentOrder());
        }

        DeliveryShipment saved = shipmentRepository.save(shipment);
        eventPublisher.publishDeliveryStatusUpdated(
            saved.getShipmentId(), saved.getReplenishmentOrder().getReplenishmentOrderId(), request.statusCode());
        return DeliveryMapper.toDto(saved);
    }

    private void receiveIntoInventory(ReplenishmentOrder order) {
        for (ReplenishmentOrderLine line : order.getLines()) {
            StoreInventory inventory = storeInventoryRepository
                .findByStoreIdAndProductId(order.getStore().getStoreId(), line.getProduct().getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "No store_inventory row for store " + order.getStore().getStoreId()
                        + " / product " + line.getProduct().getProductId() + " to receive into"));
            inventory.setOnHandQty(inventory.getOnHandQty().add(line.getOrderedQty()));
        }
    }

    private void closeOrder(ReplenishmentOrder order) {
        RefOrderStatus delivered = orderStatusRepository.findById("DELIVERED")
            .orElseThrow(() -> new IllegalStateException("Reference status missing: DELIVERED"));
        order.setStatus(delivered);
    }

    public List<DeliveryStatusDto> findByOrderId(Long orderId) {
        List<DeliveryShipment> shipments = shipmentRepository.findByOrderId(orderId);
        if (shipments.isEmpty()) {
            throw new ResourceNotFoundException("No shipment exists yet for replenishment order: " + orderId);
        }
        return shipments.stream().map(DeliveryMapper::toDto).toList();
    }
}
