package com.portfolio.pricetracker.service.scraper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import com.portfolio.pricetracker.config.ScraperConfig;
import com.portfolio.pricetracker.dto.ScrapedProductDTO;
import com.portfolio.pricetracker.entity.ScraperType;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class MediaMarktScraper implements SiteScraper {

    private static final String SEARCH_URL = "https://www.mediamarkt.es/es/search.html";
    private static final int TIMEOUT_MS = 10_000;

    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public MediaMarktScraper(ScraperConfig scraperConfig, ObjectMapper objectMapper) {
        double rate = scraperConfig.getRateLimitForSite("mediamarkt");
        this.rateLimiter = RateLimiter.create(rate);
        this.objectMapper = objectMapper;
    }

    @Override
    public String getSiteName() {
        return "MediaMarkt ES";
    }

    @Override
    public ScraperType getScraperType() {
        return ScraperType.MEDIAMARKT;
    }

    @Override
    public List<ScrapedProductDTO> scrape(String keyword, String category) {
        try {
            Document doc = fetchSearchPage(keyword);
            return parseSearchResults(doc);
        } catch (IOException e) {
            log.error("Error scraping MediaMarkt for keyword '{}': {}", keyword, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Retryable(
            retryFor = IOException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Document fetchSearchPage(String keyword) throws IOException {
        rateLimiter.acquire();

        String searchUrl = SEARCH_URL + "?query=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        log.info("Scraping MediaMarkt: {}", searchUrl);

        return Jsoup.connect(searchUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(TIMEOUT_MS)
                .header("Accept-Language", "es-ES,es;q=0.9")
                .header("Accept", "text/html,application/xhtml+xml")
                .get();
    }

    List<ScrapedProductDTO> parseSearchResults(Document doc) {
        Elements scripts = doc.select("script[type=application/ld+json]");

        for (Element script : scripts) {
            try {
                JsonNode root = objectMapper.readTree(script.html());

                if (root.isArray()) {
                    for (JsonNode node : root) {
                        if ("ItemList".equals(node.path("@type").asText())) {
                            return parseItemList(node);
                        }
                    }
                } else if ("ItemList".equals(root.path("@type").asText())) {
                    return parseItemList(root);
                }
            } catch (JsonProcessingException e) {
                log.debug("Failed to parse JSON-LD script: {}", e.getMessage());
            }
        }

        log.warn("No ItemList JSON-LD found in MediaMarkt response");
        return new ArrayList<>();
    }

    private List<ScrapedProductDTO> parseItemList(JsonNode root) {
        return StreamSupport.stream(root.path("itemListElement").spliterator(), false)
                .map(listItem -> {
                    // Schema.org can wrap each entry as ListItem { item: Product }
                    JsonNode product = listItem.has("item") ? listItem.path("item") : listItem;
                    return parseProduct(product);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    ScrapedProductDTO parseProduct(JsonNode item) {
        try {
            String name = item.path("name").asText(null);
            if (name == null || name.isBlank()) return null;

            BigDecimal price = extractPrice(item);
            String url = item.path("url").asText(null);
            String imageUrl = item.path("image").asText(null);

            return ScrapedProductDTO.builder()
                    .name(name)
                    .price(price)
                    .url(url)
                    .imageUrl(imageUrl)
                    .inStock(price != null)
                    .build();
        } catch (Exception e) {
            log.debug("Failed to parse MediaMarkt product: {}", e.getMessage());
            return null;
        }
    }

    private BigDecimal extractPrice(JsonNode item) {
        JsonNode priceNode = item.path("offers").path("price");
        if (priceNode.isMissingNode() || priceNode.isNull()) return null;
        try {
            return new BigDecimal(priceNode.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
