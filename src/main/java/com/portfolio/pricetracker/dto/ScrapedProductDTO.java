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
public class ScrapedProductDTO {

    private String name;
    private BigDecimal price;
    private String url;
    private String imageUrl;
    private Boolean inStock;
}
