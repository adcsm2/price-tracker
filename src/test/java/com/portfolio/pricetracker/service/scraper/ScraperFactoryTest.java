package com.portfolio.pricetracker.service.scraper;

import com.portfolio.pricetracker.config.ScraperConfig;
import com.portfolio.pricetracker.entity.ScraperType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScraperFactoryTest {

    private ScraperFactory factory;

    @BeforeEach
    void setUp() {
        ScraperConfig config = new ScraperConfig();
        config.setRateLimit(Map.of("amazon", 10.0, "pccomponentes", 10.0));
        ScraperConfig.PccomponentesConfig pcConfig = new ScraperConfig.PccomponentesConfig();
        pcConfig.setApiUrl("https://www.pccomponentes.com/api/v1/search");
        config.setPccomponentes(pcConfig);

        AmazonScraper amazonScraper = new AmazonScraper(config);
        PCComponentesScraper pcScraper = new PCComponentesScraper(config, new RestTemplate());

        factory = new ScraperFactory(List.of(amazonScraper, pcScraper));
    }

    @Test
    void should_ReturnAmazonScraper_When_TypeIsAmazon() {
        SiteScraper scraper = factory.getScraper(ScraperType.AMAZON);
        assertThat(scraper).isInstanceOf(AmazonScraper.class);
    }

    @Test
    void should_ReturnPCComponentesScraper_When_TypeIsPCComponentes() {
        SiteScraper scraper = factory.getScraper(ScraperType.PCCOMPONENTES);
        assertThat(scraper).isInstanceOf(PCComponentesScraper.class);
    }

    @Test
    void should_ThrowException_When_ScraperTypeNotRegistered() {
        ScraperFactory emptyFactory = new ScraperFactory(List.of());

        assertThatThrownBy(() -> emptyFactory.getScraper(ScraperType.AMAZON))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AMAZON");
    }

    @Test
    void should_ReturnAllScrapers() {
        List<SiteScraper> all = factory.getAllScrapers();
        assertThat(all).hasSize(2);
    }
}
