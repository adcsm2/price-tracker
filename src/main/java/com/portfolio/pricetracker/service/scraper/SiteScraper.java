package com.portfolio.pricetracker.service.scraper;

import com.portfolio.pricetracker.dto.ScrapedProductDTO;
import com.portfolio.pricetracker.entity.ScraperType;

import java.util.List;

public interface SiteScraper {

    String getSiteName();

    ScraperType getScraperType();

    List<ScrapedProductDTO> scrape(String keyword, String category);
}
