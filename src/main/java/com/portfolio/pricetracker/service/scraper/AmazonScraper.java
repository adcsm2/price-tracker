package com.portfolio.pricetracker.service.scraper;

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
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AmazonScraper implements SiteScraper {

    private static final String BASE_URL = "https://www.amazon.es";
    private static final int TIMEOUT_MS = 10_000;
    private static final Random RANDOM = new Random();

    private static final String[] USER_AGENTS = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15"
    };

    private final RateLimiter rateLimiter;

    public AmazonScraper(ScraperConfig scraperConfig) {
        double rate = scraperConfig.getRateLimitForSite("amazon");
        this.rateLimiter = RateLimiter.create(rate);
    }

    @Override
    public String getSiteName() {
        return "Amazon ES";
    }

    @Override
    public ScraperType getScraperType() {
        return ScraperType.AMAZON;
    }

    @Override
    public List<ScrapedProductDTO> scrape(String keyword, String category) {
        try {
            Document doc = fetchSearchPage(keyword);
            return parseSearchResults(doc);
        } catch (IOException e) {
            log.error("Error scraping Amazon for keyword '{}': {}", keyword, e.getMessage());
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

        String searchUrl = BASE_URL + "/s?k=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        log.info("Scraping Amazon: {}", searchUrl);

        return Jsoup.connect(searchUrl)
                .userAgent(getRandomUserAgent())
                .timeout(TIMEOUT_MS)
                .header("Accept-Language", "es-ES,es;q=0.9")
                .header("Accept", "text/html,application/xhtml+xml")
                .get();
    }

    List<ScrapedProductDTO> parseSearchResults(Document doc) {
        Elements items = doc.select("[data-component-type=s-search-result]");

        return items.stream()
                .map(this::parseProduct)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    ScrapedProductDTO parseProduct(Element item) {
        try {
            String name = extractName(item);
            if (name == null) return null;

            BigDecimal price = extractPrice(item);
            String url = extractUrl(item);
            String imageUrl = extractImageUrl(item);
            boolean inStock = price != null;

            return ScrapedProductDTO.builder()
                    .name(name)
                    .price(price)
                    .url(url)
                    .imageUrl(imageUrl)
                    .inStock(inStock)
                    .build();
        } catch (Exception e) {
            log.debug("Failed to parse product element: {}", e.getMessage());
            return null;
        }
    }

    private String extractName(Element item) {
        Element titleElement = item.selectFirst("h2 a span");
        if (titleElement == null) {
            titleElement = item.selectFirst("h2 span");
        }
        return titleElement != null ? titleElement.text().trim() : null;
    }

    private BigDecimal extractPrice(Element item) {
        // Primary: structured elements (.a-price-whole / .a-price-fraction)
        Element wholeEl = item.selectFirst(".a-price .a-price-whole");
        Element fractionEl = item.selectFirst(".a-price .a-price-fraction");
        if (wholeEl != null) {
            String whole = wholeEl.ownText().replace(".", "").replace(",", "").trim();
            String fraction = fractionEl != null ? fractionEl.text().trim() : "00";
            try {
                if (!whole.isEmpty()) return new BigDecimal(whole + "." + fraction);
            } catch (NumberFormatException e) {
                log.debug("Failed to parse price whole/fraction: {}.{}", whole, fraction);
            }
        }

        // Fallback: .a-offscreen (accessibility span, present even when layout varies)
        Element offscreen = item.selectFirst(".a-price .a-offscreen");
        if (offscreen != null) {
            String text = offscreen.text().replace("€", "").replace("EUR", "").trim();
            // Spanish format uses comma as decimal separator: "549,00" → "549.00"
            if (text.contains(",")) {
                text = text.replace(".", "").replace(",", ".");
            }
            try {
                if (!text.isEmpty()) return new BigDecimal(text);
            } catch (NumberFormatException e) {
                log.debug("Failed to parse offscreen price: {}", offscreen.text());
            }
        }

        return null;
    }

    private String extractUrl(Element item) {
        // Old structure: <h2><a href="...">...</a></h2>
        Element link = item.selectFirst("h2 a");
        // New structure: <a href="..."><h2>...</h2></a>
        if (link == null) {
            link = item.selectFirst("a:has(h2)");
        }
        if (link == null) return null;

        String href = link.attr("href");
        if (href.startsWith("/")) {
            return BASE_URL + href;
        }
        return href;
    }

    private String extractImageUrl(Element item) {
        Element img = item.selectFirst(".s-image");
        return img != null ? img.attr("src") : null;
    }

    private String getRandomUserAgent() {
        return USER_AGENTS[RANDOM.nextInt(USER_AGENTS.length)];
    }
}
