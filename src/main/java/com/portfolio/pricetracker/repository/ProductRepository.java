package com.portfolio.pricetracker.repository;

import com.portfolio.pricetracker.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
