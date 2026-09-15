package com.rollingstone.retailreplenishment.repository;

import com.rollingstone.retailreplenishment.entity.DeliveryTrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryTrackingEventRepository extends JpaRepository<DeliveryTrackingEvent, Long> {
}
