package com.havi.retailreplenishment.repository;

import com.havi.retailreplenishment.entity.RefOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefOrderStatusRepository extends JpaRepository<RefOrderStatus, String> {
}
