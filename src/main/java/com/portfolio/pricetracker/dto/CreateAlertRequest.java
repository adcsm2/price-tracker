package com.portfolio.pricetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAlertRequest {
    private String userEmail;
    private Long productId;
    private BigDecimal targetPrice;
}
