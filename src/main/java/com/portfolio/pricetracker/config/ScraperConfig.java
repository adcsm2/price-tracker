package com.portfolio.pricetracker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "scraper")
@EnableRetry
@EnableScheduling
@Getter
@Setter
public class ScraperConfig {

    private Map<String, Double> rateLimit;

    public double getRateLimitForSite(String site) {
        return rateLimit.getOrDefault(site, 2.0);
    }
}
