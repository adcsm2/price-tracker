package com.portfolio.pricetracker.service;

import com.portfolio.pricetracker.dto.CreateScrapingJobRequest;
import com.portfolio.pricetracker.dto.ScrapedProductDTO;
import com.portfolio.pricetracker.dto.ScrapingJobDTO;
import com.portfolio.pricetracker.entity.JobStatus;
import com.portfolio.pricetracker.entity.ScrapingJob;
import com.portfolio.pricetracker.entity.WebsiteSource;
import com.portfolio.pricetracker.repository.ScrapingJobRepository;
import com.portfolio.pricetracker.repository.WebsiteSourceRepository;
import com.portfolio.pricetracker.service.scraper.ScraperFactory;
import com.portfolio.pricetracker.service.scraper.SiteScraper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapingJobService {

    private final ScrapingJobRepository jobRepository;
    private final WebsiteSourceRepository sourceRepository;
    private final ScraperFactory scraperFactory;

    @Transactional
    public ScrapingJobDTO createJob(CreateScrapingJobRequest request) {
        WebsiteSource source = sourceRepository.findById(request.getSourceId())
                .orElseThrow(() -> new EntityNotFoundException("Source not found: " + request.getSourceId()));

        ScrapingJob job = ScrapingJob.builder()
                .source(source)
                .searchKeyword(request.getKeyword())
                .category(request.getCategory())
                .status(JobStatus.PENDING)
                .build();

        return toDTO(jobRepository.save(job));
    }

    /**
     * Executes a scraping job synchronously.
     *
     * Status updates are committed immediately via individual repository.save() calls,
     * so RUNNING/COMPLETED/FAILED states are visible to other threads without waiting
     * for a surrounding transaction to finish. The source is loaded eagerly via JOIN FETCH
     * to avoid LazyInitializationException outside of a transaction.
     *
     * Trade-off: if the application is scaled horizontally, two instances could pick up
     * the same PENDING job simultaneously. For a single-instance portfolio project this
     * is acceptable; a distributed lock (e.g. via Redis) would be needed at scale.
     */
    public ScrapingJobDTO runJob(Long jobId) {
        ScrapingJob job = jobRepository.findByIdWithSource(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + jobId));

        if (job.getStatus() != JobStatus.PENDING) {
            throw new IllegalStateException(
                    "Job " + jobId + " cannot be run: current status is " + job.getStatus());
        }

        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        jobRepository.save(job);

        try {
            SiteScraper scraper = scraperFactory.getScraper(job.getSource().getScraperType());
            List<ScrapedProductDTO> results = scraper.scrape(job.getSearchKeyword(), job.getCategory());

            job.setStatus(JobStatus.COMPLETED);
            job.setItemsFound(results.size());
            job.setCompletedAt(LocalDateTime.now());
            log.info("Job {} completed: {} items found for keyword '{}'",
                    jobId, results.size(), job.getSearchKeyword());

        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            log.error("Job {} failed for keyword '{}': {}", jobId, job.getSearchKeyword(), e.getMessage());
        }

        return toDTO(jobRepository.save(job));
    }

    @Transactional(readOnly = true)
    public List<ScrapingJobDTO> findAll() {
        return jobRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ScrapingJobDTO findById(Long id) {
        return jobRepository.findByIdWithSource(id)
                .map(this::toDTO)
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + id));
    }

    public void runPendingJobs() {
        List<ScrapingJob> pending = jobRepository.findByStatus(JobStatus.PENDING);
        log.info("Scheduled scraping: found {} pending job(s)", pending.size());
        pending.forEach(job -> runJob(job.getId()));
    }

    private ScrapingJobDTO toDTO(ScrapingJob job) {
        return ScrapingJobDTO.builder()
                .id(job.getId())
                .sourceId(job.getSource() != null ? job.getSource().getId() : null)
                .sourceName(job.getSource() != null ? job.getSource().getName() : null)
                .searchKeyword(job.getSearchKeyword())
                .category(job.getCategory())
                .status(job.getStatus())
                .itemsFound(job.getItemsFound())
                .errorMessage(job.getErrorMessage())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .createdAt(job.getCreatedAt())
                .build();
    }
}
