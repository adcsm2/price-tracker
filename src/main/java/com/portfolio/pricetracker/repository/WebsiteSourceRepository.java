package com.portfolio.pricetracker.repository;

import com.portfolio.pricetracker.entity.ScraperType;
import com.portfolio.pricetracker.entity.SourceStatus;
import com.portfolio.pricetracker.entity.WebsiteSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebsiteSourceRepository extends JpaRepository<WebsiteSource, Long> {

    Optional<WebsiteSource> findByScraperType(ScraperType scraperType);

    List<WebsiteSource> findByStatus(SourceStatus status);
}
