package com.portfolio.pricetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductListingDTO {

    private Long id;
    private Long productId;
    private Long sourceId;
    private String sourceName;
    private String externalId;
    private String url;
    private BigDecimal currentPrice;
    private String currency;
    private Boolean inStock;
    private LocalDateTime lastScrapedAt;
}
