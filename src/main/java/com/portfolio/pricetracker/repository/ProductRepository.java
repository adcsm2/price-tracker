package com.portfolio.pricetracker.repository;

import com.portfolio.pricetracker.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL")
    List<Product> findAllActive();

    @Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL AND p.id = :id")
    Optional<Product> findActiveById(@Param("id") Long id);

    List<Product> findByCategoryAndDeletedAtIsNull(String category);

    @Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT DISTINCT p FROM Product p JOIN p.listings l WHERE p.deletedAt IS NULL AND l.currentPrice >= :minPrice")
    List<Product> findByMinPrice(@Param("minPrice") BigDecimal minPrice);

    @Query("SELECT DISTINCT p FROM Product p JOIN p.listings l WHERE p.deletedAt IS NULL AND l.currentPrice <= :maxPrice")
    List<Product> findByMaxPrice(@Param("maxPrice") BigDecimal maxPrice);

    @Query("SELECT DISTINCT p FROM Product p JOIN p.listings l WHERE p.deletedAt IS NULL AND l.currentPrice BETWEEN :minPrice AND :maxPrice")
    List<Product> findByPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);
}
