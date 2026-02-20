package com.portfolio.pricetracker.repository;

import com.portfolio.pricetracker.entity.ProductListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductListingRepository extends JpaRepository<ProductListing, Long> {

    List<ProductListing> findByProductId(Long productId);

    Optional<ProductListing> findByProductIdAndSourceId(Long productId, Long sourceId);

    @Query("SELECT pl FROM ProductListing pl WHERE pl.product.id = :productId ORDER BY pl.currentPrice ASC")
    List<ProductListing> findByProductIdOrderByPriceAsc(@Param("productId") Long productId);

    Optional<ProductListing> findByUrl(String url);
}
