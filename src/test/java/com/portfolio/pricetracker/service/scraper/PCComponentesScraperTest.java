package com.portfolio.pricetracker.service.scraper;

import com.portfolio.pricetracker.config.ScraperConfig;
import com.portfolio.pricetracker.dto.ScrapedProductDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PCComponentesScraperTest {

    private PCComponentesScraper scraper;

    @BeforeEach
    void setUp() {
        ScraperConfig config = new ScraperConfig();
        config.setRateLimit(Map.of("pccomponentes", 10.0));
        ScraperConfig.PccomponentesConfig pcConfig = new ScraperConfig.PccomponentesConfig();
        pcConfig.setApiUrl("https://www.pccomponentes.com/api/v1/search");
        config.setPccomponentes(pcConfig);
        scraper = new PCComponentesScraper(config, new RestTemplate());
    }

    @Test
    void should_ReturnCorrectSiteName() {
        assertThat(scraper.getSiteName()).isEqualTo("PCComponentes");
    }

    @Test
    void should_ReturnCorrectScraperType() {
        assertThat(scraper.getScraperType().name()).isEqualTo("PCCOMPONENTES");
    }

    @Test
    void should_ParseProducts_When_ValidResponse() {
        Map<String, Object> response = Map.of(
                "data", List.of(
                        Map.of("name", "NVIDIA RTX 4070 SUPER", "price", "599.99",
                                "slug", "nvidia-rtx-4070-super", "photo", "https://img.pccomponentes.com/test.jpg"),
                        Map.of("name", "MSI RTX 4070 SUPER VENTUS", "price", "639.00",
                                "slug", "msi-rtx-4070-super", "photo", "https://img.pccomponentes.com/test2.jpg")
                )
        );

        List<ScrapedProductDTO> products = scraper.parseResponse(response);

        assertThat(products).hasSize(2);
    }

    @Test
    void should_ExtractProductName() {
        Map<String, Object> item = Map.of(
                "name", "NVIDIA RTX 4070 SUPER 12GB",
                "price", "599.99",
                "slug", "nvidia-rtx-4070"
        );

        ScrapedProductDTO dto = scraper.parseProduct(item);

        assertThat(dto).isNotNull();
        assertThat(dto.getName()).isEqualTo("NVIDIA RTX 4070 SUPER 12GB");
    }

    @Test
    void should_ExtractPrice() {
        Map<String, Object> item = Map.of(
                "name", "RTX 4070",
                "price", "599.99",
                "slug", "rtx-4070"
        );

        ScrapedProductDTO dto = scraper.parseProduct(item);

        assertThat(dto.getPrice()).isEqualByComparingTo(new BigDecimal("599.99"));
        assertThat(dto.getInStock()).isTrue();
    }

    @Test
    void should_SetInStockFalse_When_NoPrice() {
        Map<String, Object> item = Map.of(
                "name", "RTX 4070",
                "slug", "rtx-4070"
        );

        ScrapedProductDTO dto = scraper.parseProduct(item);

        assertThat(dto.getPrice()).isNull();
        assertThat(dto.getInStock()).isFalse();
    }

    @Test
    void should_BuildCorrectUrl() {
        Map<String, Object> item = Map.of(
                "name", "RTX 4070",
                "price", "599.99",
                "slug", "nvidia-rtx-4070-super-12gb"
        );

        ScrapedProductDTO dto = scraper.parseProduct(item);

        assertThat(dto.getUrl()).isEqualTo("https://www.pccomponentes.com/nvidia-rtx-4070-super-12gb");
    }

    @Test
    void should_ReturnEmpty_When_NullResponse() {
        List<ScrapedProductDTO> products = scraper.parseResponse(null);
        assertThat(products).isEmpty();
    }

    @Test
    void should_ReturnEmpty_When_NoDataKey() {
        List<ScrapedProductDTO> products = scraper.parseResponse(Map.of("total", 0));
        assertThat(products).isEmpty();
    }

    @Test
    void should_FilterOutItemsWithNoName() {
        Map<String, Object> response = Map.of(
                "data", List.of(
                        Map.of("price", "599.99", "slug", "some-product"),
                        Map.of("name", "RTX 4070", "price", "599.99", "slug", "rtx-4070")
                )
        );

        List<ScrapedProductDTO> products = scraper.parseResponse(response);

        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("RTX 4070");
    }
}
