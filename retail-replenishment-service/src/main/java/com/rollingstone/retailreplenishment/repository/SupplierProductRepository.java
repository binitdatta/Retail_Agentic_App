package com.rollingstone.retailreplenishment.repository;

import com.rollingstone.retailreplenishment.entity.SupplierProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SupplierProductRepository extends JpaRepository<SupplierProduct, Long> {

    @Query("""
        SELECT sp FROM SupplierProduct sp
        JOIN FETCH sp.supplier s
        WHERE sp.product.productId = :productId
          AND s.active = true
        ORDER BY sp.preferred DESC, s.avgLeadTimeDays ASC
        """)
    List<SupplierProduct> findAvailableSuppliersForProduct(Long productId);
}
