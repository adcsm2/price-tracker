package com.portfolio.pricetracker.service;

import com.portfolio.pricetracker.dto.ProductDTO;
import com.portfolio.pricetracker.entity.Product;
import com.portfolio.pricetracker.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public ProductDTO create(ProductDTO dto) {
        Product product = Product.builder()
                .name(dto.getName())
                .category(dto.getCategory())
                .imageUrl(dto.getImageUrl())
                .build();
        return toDTO(productRepository.save(product));
    }

    public List<ProductDTO> findAll(String category, String keyword, BigDecimal minPrice, BigDecimal maxPrice) {
        List<Product> products;

        if (keyword != null && !keyword.isBlank()) {
            products = productRepository.searchByKeyword(keyword);
        } else if (category != null && !category.isBlank()) {
            products = productRepository.findByCategoryAndDeletedAtIsNull(category);
        } else if (minPrice != null && maxPrice != null) {
            products = productRepository.findByPriceRange(minPrice, maxPrice);
        } else if (minPrice != null) {
            products = productRepository.findByMinPrice(minPrice);
        } else if (maxPrice != null) {
            products = productRepository.findByMaxPrice(maxPrice);
        } else {
            products = productRepository.findAllActive();
        }

        return products.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public ProductDTO findById(Long id) {
        Product product = productRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
        return toDTO(product);
    }

    @Transactional
    public ProductDTO update(Long id, ProductDTO dto) {
        Product product = productRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));

        product.setName(dto.getName());
        product.setCategory(dto.getCategory());
        product.setImageUrl(dto.getImageUrl());

        return toDTO(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
        product.setDeletedAt(LocalDateTime.now());
        productRepository.save(product);
    }

    private ProductDTO toDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
