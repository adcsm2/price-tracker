package com.portfolio.pricetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceDropDTO {

    private Long productId;
    private String productName;
    private Long listingId;
    private String sourceName;
    private String url;
    private BigDecimal previousPrice;
    private BigDecimal currentPrice;
    private BigDecimal priceChange;       // negative = dropped, positive = increased
    private double changePercentage;      // negative = dropped, positive = increased
}
