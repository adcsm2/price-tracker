package com.portfolio.pricetracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledScrapingService {

    private final ScrapingJobService scrapingJobService;

    @Scheduled(cron = "0 0 2 * * *")
    public void runDailyScraping() {
        log.info("Starting scheduled daily scraping at 2 AM");
        scrapingJobService.runPendingJobs();
        log.info("Scheduled daily scraping finished");
    }
}
