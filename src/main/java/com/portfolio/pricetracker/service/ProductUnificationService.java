package com.portfolio.pricetracker.service;

import com.portfolio.pricetracker.dto.ScrapedProductDTO;
import com.portfolio.pricetracker.entity.*;
import com.portfolio.pricetracker.repository.PriceHistoryRepository;
import com.portfolio.pricetracker.repository.ProductListingRepository;
import com.portfolio.pricetracker.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductUnificationService {

    private final ProductRepository productRepository;
    private final ProductListingRepository listingRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final TransactionTemplate transactionTemplate;

    /**
     * Each item runs in its own transaction via TransactionTemplate.
     * If one item fails (e.g. constraint violation), only that item's transaction
     * rolls back — not the entire batch. This avoids the "rollback-only" issue
     * that occurs when catching JPA exceptions inside a single @Transactional method.
     */
    public void saveResults(List<ScrapedProductDTO> results, WebsiteSource source) {
        int saved = 0;
        for (ScrapedProductDTO scraped : results) {
            if (scraped.getUrl() == null || scraped.getPrice() == null) continue;
            try {
                transactionTemplate.executeWithoutResult(status -> processScrapedProduct(scraped, source));
                saved++;
            } catch (Exception e) {
                log.warn("Failed to process scraped product '{}': {}", scraped.getName(), e.getMessage());
            }
        }
        log.info("Saved {}/{} scraped products from {}", saved, results.size(), source.getName());
    }

    private void processScrapedProduct(ScrapedProductDTO scraped, WebsiteSource source) {
        ProductListing listing = listingRepository.findByUrl(scraped.getUrl())
                .orElseGet(() -> createListing(scraped, source));

        listing.setCurrentPrice(scraped.getPrice());
        listing.setInStock(scraped.getInStock());
        listing.setLastScrapedAt(LocalDateTime.now());
        listingRepository.save(listing);

        PriceHistory history = PriceHistory.builder()
                .listing(listing)
                .product(listing.getProduct())
                .price(scraped.getPrice())
                .inStock(scraped.getInStock())
                .scrapedAt(LocalDateTime.now())
                .build();
        priceHistoryRepository.save(history);
    }

    /**
     * Creates a new ProductListing.
     *
     * Product matching strategy: find by exact name (case-insensitive) to reuse the same
     * canonical product across sites (e.g. "ASUS TUF RTX 4070" on Amazon and MediaMarkt
     * share one Product row). If no match, a new Product is created.
     *
     * If the (product, source) combination already exists (unique constraint), the existing
     * listing is reused and its URL updated to the new one.
     */
    private ProductListing createListing(ScrapedProductDTO scraped, WebsiteSource source) {
        Product product = productRepository
                .findByNameIgnoreCaseAndDeletedAtIsNull(scraped.getName())
                .orElseGet(() -> productRepository.save(Product.builder()
                        .name(scraped.getName())
                        .imageUrl(scraped.getImageUrl())
                        .build()));

        return listingRepository.findByProductIdAndSourceId(product.getId(), source.getId())
                .map(existing -> {
                    existing.setUrl(scraped.getUrl());
                    return existing;
                })
                .orElseGet(() -> ProductListing.builder()
                        .product(product)
                        .source(source)
                        .url(scraped.getUrl())
                        .currency("EUR")
                        .build());
    }
}
