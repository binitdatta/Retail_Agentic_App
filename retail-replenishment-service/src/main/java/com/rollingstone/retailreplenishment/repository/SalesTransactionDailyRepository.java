package com.rollingstone.retailreplenishment.repository;

import com.rollingstone.retailreplenishment.entity.SalesTransactionDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SalesTransactionDailyRepository extends JpaRepository<SalesTransactionDaily, Long> {

    @Query("""
        SELECT s FROM SalesTransactionDaily s
        WHERE s.store.storeId = :storeId
          AND s.product.productId = :productId
          AND s.salesDate >= :fromDate
        ORDER BY s.salesDate
        """)
    List<SalesTransactionDaily> findRecentHistory(
        @Param("storeId") Long storeId,
        @Param("productId") Long productId,
        @Param("fromDate") LocalDate fromDate);
}
