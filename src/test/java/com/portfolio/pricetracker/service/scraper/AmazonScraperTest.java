package com.portfolio.pricetracker.service.scraper;

import com.portfolio.pricetracker.config.ScraperConfig;
import com.portfolio.pricetracker.dto.ScrapedProductDTO;
import com.portfolio.pricetracker.entity.ScraperType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AmazonScraperTest {

    private AmazonScraper amazonScraper;

    @BeforeEach
    void setUp() {
        ScraperConfig config = new ScraperConfig();
        config.setRateLimit(Map.of("amazon", 10.0));
        amazonScraper = new AmazonScraper(config);
    }

    private Document loadFixture(String filename) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("fixtures/" + filename);
        assertThat(is).isNotNull();
        String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        return Jsoup.parse(html);
    }

    @Test
    void should_ReturnCorrectSiteName() {
        assertThat(amazonScraper.getSiteName()).isEqualTo("Amazon ES");
    }

    @Test
    void should_ReturnCorrectScraperType() {
        assertThat(amazonScraper.getScraperType()).isEqualTo(ScraperType.AMAZON);
    }

    @Test
    void should_ParseProducts_When_ValidHtml() throws IOException {
        Document doc = loadFixture("amazon_search.html");

        List<ScrapedProductDTO> products = amazonScraper.parseSearchResults(doc);

        assertThat(products).hasSize(3);
    }

    @Test
    void should_ExtractProductName_When_ValidElement() throws IOException {
        Document doc = loadFixture("amazon_search.html");

        List<ScrapedProductDTO> products = amazonScraper.parseSearchResults(doc);

        assertThat(products.get(0).getName()).contains("RTX 4070 SUPER");
        assertThat(products.get(1).getName()).contains("MSI");
    }

    @Test
    void should_ExtractPrice_When_PriceAvailable() throws IOException {
        Document doc = loadFixture("amazon_search.html");

        List<ScrapedProductDTO> products = amazonScraper.parseSearchResults(doc);

        assertThat(products.get(0).getPrice()).isEqualByComparingTo(new BigDecimal("599.99"));
        assertThat(products.get(1).getPrice()).isEqualByComparingTo(new BigDecimal("649.00"));
    }

    @Test
    void should_SetInStockFalse_When_NoPriceAvailable() throws IOException {
        Document doc = loadFixture("amazon_search.html");

        List<ScrapedProductDTO> products = amazonScraper.parseSearchResults(doc);

        ScrapedProductDTO outOfStock = products.stream()
                .filter(p -> p.getName().contains("ASUS"))
                .findFirst()
                .orElseThrow();

        assertThat(outOfStock.getPrice()).isNull();
        assertThat(outOfStock.getInStock()).isFalse();
    }

    @Test
    void should_ExtractUrl_When_LinkPresent() throws IOException {
        Document doc = loadFixture("amazon_search.html");

        List<ScrapedProductDTO> products = amazonScraper.parseSearchResults(doc);

        assertThat(products.get(0).getUrl()).contains("/dp/B0TEST001");
        assertThat(products.get(0).getUrl()).startsWith("https://www.amazon.es");
    }

    @Test
    void should_ExtractImageUrl_When_ImagePresent() throws IOException {
        Document doc = loadFixture("amazon_search.html");

        List<ScrapedProductDTO> products = amazonScraper.parseSearchResults(doc);

        assertThat(products.get(0).getImageUrl()).contains("test1.jpg");
    }

    @Test
    void should_FilterOutInvalidElements() throws IOException {
        Document doc = loadFixture("amazon_search.html");

        List<ScrapedProductDTO> products = amazonScraper.parseSearchResults(doc);

        // Product 4 has no h2/name, should be filtered
        assertThat(products).noneMatch(p -> p.getName() == null);
    }

    @Test
    void should_HandleEmptyResults() {
        Document doc = Jsoup.parse("<html><body></body></html>");

        List<ScrapedProductDTO> products = amazonScraper.parseSearchResults(doc);

        assertThat(products).isEmpty();
    }
}
