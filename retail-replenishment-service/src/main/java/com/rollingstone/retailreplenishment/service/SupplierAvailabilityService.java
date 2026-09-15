package com.rollingstone.retailreplenishment.service;

import com.rollingstone.retailreplenishment.dto.SupplierAvailabilityDto;
import com.rollingstone.retailreplenishment.mapper.SupplierProductMapper;
import com.rollingstone.retailreplenishment.repository.SupplierProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Backs the "check supplier availability" stage.
 */
@Service
@Transactional(readOnly = true)
public class SupplierAvailabilityService {

    private final SupplierProductRepository supplierProductRepository;

    public SupplierAvailabilityService(SupplierProductRepository supplierProductRepository) {
        this.supplierProductRepository = supplierProductRepository;
    }

    public List<SupplierAvailabilityDto> findAvailability(Long productId) {
        return supplierProductRepository.findAvailableSuppliersForProduct(productId).stream()
            .map(SupplierProductMapper::toDto)
            .toList();
    }
}
