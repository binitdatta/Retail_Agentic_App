package com.rollingstone.retailreplenishment.service;

import com.rollingstone.retailreplenishment.dto.SupplierDto;
import com.rollingstone.retailreplenishment.entity.Supplier;
import com.rollingstone.retailreplenishment.exception.ResourceNotFoundException;
import com.rollingstone.retailreplenishment.mapper.SupplierMapper;
import com.rollingstone.retailreplenishment.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public List<SupplierDto> findAll() {
        return supplierRepository.findAll().stream().map(SupplierMapper::toDto).toList();
    }

    public SupplierDto findById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));
        return SupplierMapper.toDto(supplier);
    }
}
