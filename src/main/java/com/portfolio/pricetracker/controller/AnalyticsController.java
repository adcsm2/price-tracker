package com.portfolio.pricetracker.controller;

import com.portfolio.pricetracker.dto.PriceComparisonDTO;
import com.portfolio.pricetracker.dto.PriceDropDTO;
import com.portfolio.pricetracker.dto.TrendingProductDTO;
import com.portfolio.pricetracker.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/price-drops")
    public List<PriceDropDTO> getPriceDrops(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int limit) {
        return analyticsService.getTopPriceDrops(days, limit);
    }

    @GetMapping("/price-increases")
    public List<PriceDropDTO> getPriceIncreases(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int limit) {
        return analyticsService.getTopPriceIncreases(days, limit);
    }

    @GetMapping("/trending")
    public List<TrendingProductDTO> getTrending(
            @RequestParam(defaultValue = "10") int limit) {
        return analyticsService.getTrending(limit);
    }

    @GetMapping("/compare")
    public PriceComparisonDTO compare(@RequestParam Long productId) {
        return analyticsService.compareProduct(productId);
    }
}
