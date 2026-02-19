package com.portfolio.pricetracker.service.scraper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.pricetracker.config.ScraperConfig;
import com.portfolio.pricetracker.dto.ScrapedProductDTO;
import com.portfolio.pricetracker.entity.ScraperType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MediaMarktScraperTest {

    private MediaMarktScraper scraper;

    @BeforeEach
    void setUp() {
        ScraperConfig config = new ScraperConfig();
        config.setRateLimit(Map.of("mediamarkt", 10.0));
        scraper = new MediaMarktScraper(config, new ObjectMapper());
    }

    private Document loadFixture() throws IOException {
        return Jsoup.parse(
                getClass().getResourceAsStream("/fixtures/mediamarkt_search.html"),
                "UTF-8",
                "https://www.mediamarkt.es"
        );
    }

    @Test
    void should_ReturnCorrectSiteName() {
        assertThat(scraper.getSiteName()).isEqualTo("MediaMarkt ES");
    }

    @Test
    void should_ReturnCorrectScraperType() {
        assertThat(scraper.getScraperType()).isEqualTo(ScraperType.MEDIAMARKT);
    }

    @Test
    void should_ParseProducts_When_ValidJsonLd() throws IOException {
        Document doc = loadFixture();

        List<ScrapedProductDTO> products = scraper.parseSearchResults(doc);

        // fixture has 3 items; one has no price → inStock=false but still included
        assertThat(products).hasSize(3);
    }

    @Test
    void should_ParseNameAndPrice_Correctly() throws IOException {
        Document doc = loadFixture();

        List<ScrapedProductDTO> products = scraper.parseSearchResults(doc);
        ScrapedProductDTO first = products.get(0);

        assertThat(first.getName()).isEqualTo("ASUS TUF Gaming GeForce RTX 4070 SUPER OC 12GB GDDR6X");
        assertThat(first.getPrice()).isEqualByComparingTo(new BigDecimal("619.0"));
    }

    @Test
    void should_ParseUrl_Correctly() throws IOException {
        Document doc = loadFixture();

        List<ScrapedProductDTO> products = scraper.parseSearchResults(doc);

        assertThat(products.get(0).getUrl())
                .isEqualTo("https://www.mediamarkt.es/es/product/_asus-tuf-gaming-rtx4070super.html");
    }

    @Test
    void should_ParseImageUrl_Correctly() throws IOException {
        Document doc = loadFixture();

        List<ScrapedProductDTO> products = scraper.parseSearchResults(doc);

        assertThat(products.get(0).getImageUrl()).contains("assets.mmsrg.com");
    }

    @Test
    void should_SetInStockTrue_When_PricePresent() throws IOException {
        Document doc = loadFixture();

        List<ScrapedProductDTO> products = scraper.parseSearchResults(doc);

        assertThat(products.get(0).getInStock()).isTrue();
    }

    @Test
    void should_SetInStockFalse_When_PriceMissing() throws IOException {
        Document doc = loadFixture();

        List<ScrapedProductDTO> products = scraper.parseSearchResults(doc);
        ScrapedProductDTO noPrice = products.get(2);

        assertThat(noPrice.getInStock()).isFalse();
        assertThat(noPrice.getPrice()).isNull();
    }

    @Test
    void should_ReturnEmpty_When_NoJsonLd() {
        Document doc = Jsoup.parse("<html><body><h1>Sin resultados</h1></body></html>");

        assertThat(scraper.parseSearchResults(doc)).isEmpty();
    }

    @Test
    void should_ReturnEmpty_When_JsonLdIsNotItemList() {
        Document doc = Jsoup.parse("""
                <html><head>
                <script type="application/ld+json">
                {"@type": "WebSite", "name": "MediaMarkt"}
                </script>
                </head></html>
                """);

        assertThat(scraper.parseSearchResults(doc)).isEmpty();
    }
}
