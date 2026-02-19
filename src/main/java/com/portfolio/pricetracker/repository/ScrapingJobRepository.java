package com.portfolio.pricetracker.repository;

import com.portfolio.pricetracker.entity.JobStatus;
import com.portfolio.pricetracker.entity.ScrapingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScrapingJobRepository extends JpaRepository<ScrapingJob, Long> {

    List<ScrapingJob> findByStatus(JobStatus status);

    List<ScrapingJob> findBySourceIdOrderByCreatedAtDesc(Long sourceId);

    // JOIN FETCH to avoid LazyInitializationException when accessing source outside a transaction
    @Query("SELECT j FROM ScrapingJob j LEFT JOIN FETCH j.source WHERE j.id = :id")
    Optional<ScrapingJob> findByIdWithSource(@Param("id") Long id);
}
