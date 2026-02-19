package com.portfolio.pricetracker.service.scraper;

import com.portfolio.pricetracker.entity.ScraperType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ScraperFactory {

    private final Map<ScraperType, SiteScraper> scrapers;

    public ScraperFactory(List<SiteScraper> scraperList) {
        this.scrapers = scraperList.stream()
                .collect(Collectors.toMap(SiteScraper::getScraperType, Function.identity()));
    }

    public SiteScraper getScraper(ScraperType type) {
        SiteScraper scraper = scrapers.get(type);
        if (scraper == null) {
            throw new IllegalArgumentException("No scraper registered for type: " + type);
        }
        return scraper;
    }

    public List<SiteScraper> getAllScrapers() {
        return List.copyOf(scrapers.values());
    }
}
