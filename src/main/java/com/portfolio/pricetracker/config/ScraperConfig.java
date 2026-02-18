package com.portfolio.pricetracker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "scraper")
@EnableRetry
@Getter
@Setter
public class ScraperConfig {

    private Map<String, Double> rateLimit;
    private PccomponentesConfig pccomponentes;

    public double getRateLimitForSite(String site) {
        return rateLimit.getOrDefault(site, 2.0);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Getter
    @Setter
    public static class PccomponentesConfig {
        private String apiUrl;
    }
}
