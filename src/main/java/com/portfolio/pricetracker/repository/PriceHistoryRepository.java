package com.portfolio.pricetracker.repository;

import com.portfolio.pricetracker.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByListingIdOrderByScrapedAtDesc(Long listingId);

    List<PriceHistory> findByProductIdOrderByScrapedAtDesc(Long productId);

    List<PriceHistory> findByListingIdAndScrapedAtBetweenOrderByScrapedAtAsc(
            Long listingId, LocalDateTime from, LocalDateTime to);

    // Oldest price entry for a listing since a given date (for price-drop analytics)
    Optional<PriceHistory> findFirstByListingIdAndScrapedAtAfterOrderByScrapedAtAsc(
            Long listingId, LocalDateTime since);

    // Count scrapes per product for trending analytics
    @Query("SELECT ph.product.id, ph.product.name, COUNT(ph) FROM PriceHistory ph " +
           "WHERE ph.product.deletedAt IS NULL " +
           "GROUP BY ph.product.id, ph.product.name " +
           "ORDER BY COUNT(ph) DESC")
    List<Object[]> findTrendingProducts(org.springframework.data.domain.Pageable pageable);
}
