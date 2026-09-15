package com.rollingstone.retailreplenishment.repository;

import com.rollingstone.retailreplenishment.entity.StoreInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoreInventoryRepository extends JpaRepository<StoreInventory, Long> {

    // Mirrors vw_low_stock_inventory from the DDL: available = on_hand - allocated <= reorder_point.
    @Query("""
        SELECT si FROM StoreInventory si
        JOIN FETCH si.store s
        JOIN FETCH si.product p
        WHERE (si.onHandQty - si.allocatedQty) <= si.reorderPoint
          AND s.active = true
          AND p.active = true
          AND (:storeId IS NULL OR s.storeId = :storeId)
        ORDER BY s.storeCode, p.skuCode
        """)
    List<StoreInventory> findLowStock(@Param("storeId") Long storeId);

    @Query("""
        SELECT si FROM StoreInventory si
        JOIN FETCH si.store s
        JOIN FETCH si.product p
        WHERE s.storeId = :storeId AND p.productId = :productId
        """)
    Optional<StoreInventory> findByStoreIdAndProductId(@Param("storeId") Long storeId, @Param("productId") Long productId);
}
