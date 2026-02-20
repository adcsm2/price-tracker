package com.portfolio.pricetracker.service;

import com.portfolio.pricetracker.entity.PriceAlert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class LogAlertNotifier implements AlertNotifier {

    @Override
    public void notify(PriceAlert alert, BigDecimal currentPrice) {
        log.info("PRICE ALERT: '{}' is now {}€ (target: {}€) — notifying {}",
                alert.getProduct().getName(),
                currentPrice,
                alert.getTargetPrice(),
                alert.getUserEmail());
    }
}
