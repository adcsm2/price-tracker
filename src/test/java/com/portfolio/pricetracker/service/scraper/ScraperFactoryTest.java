package com.portfolio.pricetracker.service.scraper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.pricetracker.config.ScraperConfig;
import com.portfolio.pricetracker.entity.ScraperType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScraperFactoryTest {

    private ScraperFactory factory;

    @BeforeEach
    void setUp() {
        ScraperConfig config = new ScraperConfig();
        config.setRateLimit(Map.of("amazon", 10.0, "mediamarkt", 10.0));

        AmazonScraper amazonScraper = new AmazonScraper(config);
        MediaMarktScraper mediaMarktScraper = new MediaMarktScraper(config, new ObjectMapper());

        factory = new ScraperFactory(List.of(amazonScraper, mediaMarktScraper));
    }

    @Test
    void should_ReturnAmazonScraper_When_TypeIsAmazon() {
        SiteScraper scraper = factory.getScraper(ScraperType.AMAZON);
        assertThat(scraper).isInstanceOf(AmazonScraper.class);
    }

    @Test
    void should_ReturnMediaMarktScraper_When_TypeIsMediaMarkt() {
        SiteScraper scraper = factory.getScraper(ScraperType.MEDIAMARKT);
        assertThat(scraper).isInstanceOf(MediaMarktScraper.class);
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
