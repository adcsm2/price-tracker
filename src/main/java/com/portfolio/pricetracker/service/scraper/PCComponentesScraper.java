package com.portfolio.pricetracker.service.scraper;

import com.google.common.util.concurrent.RateLimiter;
import com.portfolio.pricetracker.config.ScraperConfig;
import com.portfolio.pricetracker.dto.ScrapedProductDTO;
import com.portfolio.pricetracker.entity.ScraperType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PCComponentesScraper implements SiteScraper {

    private static final String BASE_URL = "https://www.pccomponentes.com";

    private final RestTemplate restTemplate;
    private final RateLimiter rateLimiter;
    private final String apiUrl;

    public PCComponentesScraper(ScraperConfig scraperConfig, RestTemplate restTemplate) {
        double rate = scraperConfig.getRateLimitForSite("pccomponentes");
        this.rateLimiter = RateLimiter.create(rate);
        this.restTemplate = restTemplate;
        this.apiUrl = scraperConfig.getPccomponentes().getApiUrl();
    }

    @Override
    public String getSiteName() {
        return "PCComponentes";
    }

    @Override
    public ScraperType getScraperType() {
        return ScraperType.PCCOMPONENTES;
    }

    @Override
    public List<ScrapedProductDTO> scrape(String keyword, String category) {
        try {
            return fetchProducts(keyword);
        } catch (RestClientException e) {
            log.error("Error scraping PCComponentes for keyword '{}': {}", keyword, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Retryable(
            retryFor = RestClientException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<ScrapedProductDTO> fetchProducts(String keyword) {
        rateLimiter.acquire();

        String url = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("query", keyword)
                .queryParam("perPage", 24)
                .queryParam("page", 1)
                .toUriString();

        log.info("Scraping PCComponentes: {}", url);

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        return parseResponse(response);
    }

    @SuppressWarnings("unchecked")
    List<ScrapedProductDTO> parseResponse(Map<String, Object> response) {
        if (response == null) return new ArrayList<>();

        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("data");
        if (items == null) return new ArrayList<>();

        List<ScrapedProductDTO> products = new ArrayList<>();
        for (Map<String, Object> item : items) {
            ScrapedProductDTO dto = parseProduct(item);
            if (dto != null) products.add(dto);
        }
        return products;
    }

    ScrapedProductDTO parseProduct(Map<String, Object> item) {
        try {
            String name = (String) item.get("name");
            if (name == null || name.isBlank()) return null;

            BigDecimal price = extractPrice(item);
            String slug = (String) item.get("slug");
            String url = slug != null ? BASE_URL + "/" + slug : null;
            String imageUrl = (String) item.get("photo");
            boolean inStock = price != null;

            return ScrapedProductDTO.builder()
                    .name(name)
                    .price(price)
                    .url(url)
                    .imageUrl(imageUrl)
                    .inStock(inStock)
                    .build();
        } catch (Exception e) {
            log.debug("Failed to parse PCComponentes product: {}", e.getMessage());
            return null;
        }
    }

    private BigDecimal extractPrice(Map<String, Object> item) {
        Object priceObj = item.get("price");
        if (priceObj == null) return null;
        try {
            return new BigDecimal(priceObj.toString().replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
