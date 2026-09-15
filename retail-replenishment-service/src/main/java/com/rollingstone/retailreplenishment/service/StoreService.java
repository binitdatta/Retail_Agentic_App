package com.rollingstone.retailreplenishment.service;

import com.rollingstone.retailreplenishment.dto.StoreDto;
import com.rollingstone.retailreplenishment.entity.Store;
import com.rollingstone.retailreplenishment.exception.ResourceNotFoundException;
import com.rollingstone.retailreplenishment.mapper.StoreMapper;
import com.rollingstone.retailreplenishment.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;

    public StoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public List<StoreDto> findAll() {
        return storeRepository.findAll().stream().map(StoreMapper::toDto).toList();
    }

    public StoreDto findById(Long id) {
        Store store = storeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + id));
        return StoreMapper.toDto(store);
    }
}
