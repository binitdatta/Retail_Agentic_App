package com.rollingstone.retailreplenishment.repository;

import com.rollingstone.retailreplenishment.entity.DeliveryShipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeliveryShipmentRepository extends JpaRepository<DeliveryShipment, Long> {

    @Query("""
        SELECT DISTINCT s FROM DeliveryShipment s
        JOIN FETCH s.status
        LEFT JOIN FETCH s.trackingEvents
        WHERE s.replenishmentOrder.replenishmentOrderId = :orderId
        ORDER BY s.shipmentId DESC
        """)
    List<DeliveryShipment> findByOrderId(@Param("orderId") Long orderId);

    @Query("""
        SELECT s FROM DeliveryShipment s
        JOIN FETCH s.status
        JOIN FETCH s.replenishmentOrder o
        WHERE s.shipmentId = :shipmentId
        """)
    Optional<DeliveryShipment> findByIdWithOrder(@Param("shipmentId") Long shipmentId);
}
