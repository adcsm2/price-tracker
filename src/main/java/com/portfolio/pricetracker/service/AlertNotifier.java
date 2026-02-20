package com.portfolio.pricetracker.service;

import com.portfolio.pricetracker.entity.PriceAlert;

import java.math.BigDecimal;

public interface AlertNotifier {
    void notify(PriceAlert alert, BigDecimal currentPrice);
}
