package com.rollingstone.retailreplenishment.repository;

import com.rollingstone.retailreplenishment.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findByStoreCode(String storeCode);
}
