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
public class TrendingProductDTO {

    private Long productId;
    private String productName;
    private long scrapeCount;
    private BigDecimal lowestCurrentPrice;
}
