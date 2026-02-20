package com.portfolio.pricetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceComparisonDTO {

    private Long productId;
    private String productName;
    private List<ListingPriceDTO> listings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListingPriceDTO {
        private Long listingId;
        private String sourceName;
        private BigDecimal currentPrice;
        private Boolean inStock;
        private String url;
        private LocalDateTime lastScrapedAt;
    }
}
