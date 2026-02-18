package com.portfolio.pricetracker.service;

import com.portfolio.pricetracker.dto.ProductDTO;
import com.portfolio.pricetracker.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class ProductServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    void should_CreateProduct_When_ValidDTO() {
        ProductDTO dto = ProductDTO.builder()
                .name("NVIDIA RTX 4070 SUPER")
                .category("gpu")
                .build();

        ProductDTO saved = productService.create(dto);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("NVIDIA RTX 4070 SUPER");
        assertThat(saved.getCategory()).isEqualTo("gpu");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void should_FindAllActiveProducts() {
        productService.create(ProductDTO.builder().name("RTX 4070").category("gpu").build());
        productService.create(ProductDTO.builder().name("RTX 4080").category("gpu").build());

        List<ProductDTO> products = productService.findAll(null, null, null, null);

        assertThat(products).hasSize(2);
    }

    @Test
    void should_FilterByCategory() {
        productService.create(ProductDTO.builder().name("RTX 4070").category("gpu").build());
        productService.create(ProductDTO.builder().name("Dell XPS 15").category("laptop").build());

        List<ProductDTO> gpus = productService.findAll("gpu", null, null, null);

        assertThat(gpus).hasSize(1);
        assertThat(gpus.get(0).getName()).isEqualTo("RTX 4070");
    }

    @Test
    void should_SearchByKeyword() {
        productService.create(ProductDTO.builder().name("NVIDIA RTX 4070 SUPER").category("gpu").build());
        productService.create(ProductDTO.builder().name("AMD RX 7800 XT").category("gpu").build());

        List<ProductDTO> results = productService.findAll(null, "RTX", null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).contains("RTX");
    }

    @Test
    void should_FindById_When_ProductExists() {
        ProductDTO created = productService.create(
                ProductDTO.builder().name("RTX 4070").category("gpu").build());

        ProductDTO found = productService.findById(created.getId());

        assertThat(found.getName()).isEqualTo("RTX 4070");
    }

    @Test
    void should_ThrowException_When_ProductNotFound() {
        assertThatThrownBy(() -> productService.findById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void should_UpdateProduct() {
        ProductDTO created = productService.create(
                ProductDTO.builder().name("RTX 4070").category("gpu").build());

        ProductDTO updateDTO = ProductDTO.builder()
                .name("RTX 4070 SUPER")
                .category("gpu")
                .build();

        ProductDTO updated = productService.update(created.getId(), updateDTO);

        assertThat(updated.getName()).isEqualTo("RTX 4070 SUPER");
    }

    @Test
    void should_SoftDeleteProduct() {
        ProductDTO created = productService.create(
                ProductDTO.builder().name("RTX 4070").category("gpu").build());

        productService.delete(created.getId());

        assertThatThrownBy(() -> productService.findById(created.getId()))
                .isInstanceOf(EntityNotFoundException.class);

        // Still exists in DB, just soft-deleted
        assertThat(productRepository.findById(created.getId())).isPresent();
    }

    @Test
    void should_NotReturnDeletedProducts_When_FindAll() {
        ProductDTO p1 = productService.create(
                ProductDTO.builder().name("RTX 4070").category("gpu").build());
        productService.create(
                ProductDTO.builder().name("RTX 4080").category("gpu").build());

        productService.delete(p1.getId());

        List<ProductDTO> products = productService.findAll(null, null, null, null);
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("RTX 4080");
    }
}
