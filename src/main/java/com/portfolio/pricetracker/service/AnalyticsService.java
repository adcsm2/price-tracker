package com.portfolio.pricetracker.service;

import com.portfolio.pricetracker.dto.PriceComparisonDTO;
import com.portfolio.pricetracker.dto.PriceDropDTO;
import com.portfolio.pricetracker.dto.TrendingProductDTO;
import com.portfolio.pricetracker.entity.PriceHistory;
import com.portfolio.pricetracker.entity.ProductListing;
import com.portfolio.pricetracker.repository.PriceHistoryRepository;
import com.portfolio.pricetracker.repository.ProductListingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ProductListingRepository listingRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    @Transactional(readOnly = true)
    public List<PriceDropDTO> getTopPriceDrops(int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return listingRepository.findAll().stream()
                .filter(l -> l.getCurrentPrice() != null)
                .map(l -> buildPriceChangeDTO(l, since))
                .filter(Objects::nonNull)
                .filter(dto -> dto.getPriceChange().compareTo(BigDecimal.ZERO) < 0)
                .sorted(Comparator.comparingDouble(PriceDropDTO::getChangePercentage))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PriceDropDTO> getTopPriceIncreases(int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return listingRepository.findAll().stream()
                .filter(l -> l.getCurrentPrice() != null)
                .map(l -> buildPriceChangeDTO(l, since))
                .filter(Objects::nonNull)
                .filter(dto -> dto.getPriceChange().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparingDouble(PriceDropDTO::getChangePercentage).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TrendingProductDTO> getTrending(int limit) {
        List<Object[]> rows = priceHistoryRepository.findTrendingProducts(PageRequest.of(0, limit));
        return rows.stream()
                .map(row -> {
                    Long productId = (Long) row[0];
                    String productName = (String) row[1];
                    long count = (Long) row[2];
                    BigDecimal lowestPrice = listingRepository
                            .findByProductIdOrderByPriceAsc(productId).stream()
                            .map(ProductListing::getCurrentPrice)
                            .filter(Objects::nonNull)
                            .min(Comparator.naturalOrder())
                            .orElse(null);
                    return TrendingProductDTO.builder()
                            .productId(productId)
                            .productName(productName)
                            .scrapeCount(count)
                            .lowestCurrentPrice(lowestPrice)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PriceComparisonDTO compareProduct(Long productId) {
        List<ProductListing> listings = listingRepository.findByProductIdOrderByPriceAsc(productId);
        if (listings.isEmpty()) {
            throw new EntityNotFoundException("No listings found for product: " + productId);
        }

        String productName = listings.get(0).getProduct().getName();

        List<PriceComparisonDTO.ListingPriceDTO> listingDTOs = listings.stream()
                .map(l -> PriceComparisonDTO.ListingPriceDTO.builder()
                        .listingId(l.getId())
                        .sourceName(l.getSource().getName())
                        .currentPrice(l.getCurrentPrice())
                        .inStock(l.getInStock())
                        .url(l.getUrl())
                        .lastScrapedAt(l.getLastScrapedAt())
                        .build())
                .collect(Collectors.toList());

        return PriceComparisonDTO.builder()
                .productId(productId)
                .productName(productName)
                .listings(listingDTOs)
                .build();
    }

    private PriceDropDTO buildPriceChangeDTO(ProductListing listing, LocalDateTime since) {
        Optional<PriceHistory> oldest = priceHistoryRepository
                .findFirstByListingIdAndScrapedAtAfterOrderByScrapedAtAsc(listing.getId(), since);

        if (oldest.isEmpty()) return null;

        BigDecimal previousPrice = oldest.get().getPrice();
        BigDecimal currentPrice = listing.getCurrentPrice();

        if (previousPrice.compareTo(BigDecimal.ZERO) == 0) return null;

        BigDecimal priceChange = currentPrice.subtract(previousPrice);
        double changePercentage = priceChange
                .divide(previousPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();

        return PriceDropDTO.builder()
                .productId(listing.getProduct().getId())
                .productName(listing.getProduct().getName())
                .listingId(listing.getId())
                .sourceName(listing.getSource().getName())
                .url(listing.getUrl())
                .previousPrice(previousPrice)
                .currentPrice(currentPrice)
                .priceChange(priceChange)
                .changePercentage(changePercentage)
                .build();
    }
}
