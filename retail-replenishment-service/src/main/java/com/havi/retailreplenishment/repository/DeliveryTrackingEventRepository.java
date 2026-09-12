package com.havi.retailreplenishment.repository;

import com.havi.retailreplenishment.entity.DeliveryTrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryTrackingEventRepository extends JpaRepository<DeliveryTrackingEvent, Long> {
}
