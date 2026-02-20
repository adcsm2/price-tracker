package com.portfolio.pricetracker.dto;

import com.portfolio.pricetracker.entity.AlertStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PriceAlertDTO {
    private Long id;
    private String userEmail;
    private Long productId;
    private String productName;
    private BigDecimal targetPrice;
    private AlertStatus status;
    private LocalDateTime triggeredAt;
    private LocalDateTime createdAt;
}
