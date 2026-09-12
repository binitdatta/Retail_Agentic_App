package com.havi.retailreplenishment.service;

import com.havi.retailreplenishment.dto.ProductDto;
import com.havi.retailreplenishment.entity.Product;
import com.havi.retailreplenishment.exception.ResourceNotFoundException;
import com.havi.retailreplenishment.mapper.ProductMapper;
import com.havi.retailreplenishment.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductDto> findAll() {
        return productRepository.findAll().stream().map(ProductMapper::toDto).toList();
    }

    public ProductDto findById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        return ProductMapper.toDto(product);
    }
}
