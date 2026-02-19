package com.portfolio.pricetracker.repository;

import com.portfolio.pricetracker.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByListingIdOrderByScrapedAtDesc(Long listingId);

    List<PriceHistory> findByProductIdOrderByScrapedAtDesc(Long productId);

    List<PriceHistory> findByListingIdAndScrapedAtBetweenOrderByScrapedAtAsc(
            Long listingId, LocalDateTime from, LocalDateTime to);
}
