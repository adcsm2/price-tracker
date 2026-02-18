package com.portfolio.pricetracker.repository;

import com.portfolio.pricetracker.entity.JobStatus;
import com.portfolio.pricetracker.entity.ScrapingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScrapingJobRepository extends JpaRepository<ScrapingJob, Long> {

    List<ScrapingJob> findByStatus(JobStatus status);

    List<ScrapingJob> findBySourceIdOrderByCreatedAtDesc(Long sourceId);
}
