package com.portfolio.pricetracker.controller;

import com.portfolio.pricetracker.dto.CreateScrapingJobRequest;
import com.portfolio.pricetracker.dto.ScrapingJobDTO;
import com.portfolio.pricetracker.service.ScrapingJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scraping/jobs")
@RequiredArgsConstructor
public class ScrapingJobController {

    private final ScrapingJobService scrapingJobService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScrapingJobDTO create(@RequestBody @Valid CreateScrapingJobRequest request) {
        return scrapingJobService.createJob(request);
    }

    @GetMapping
    public List<ScrapingJobDTO> findAll() {
        return scrapingJobService.findAll();
    }

    @GetMapping("/{id}")
    public ScrapingJobDTO findById(@PathVariable Long id) {
        return scrapingJobService.findById(id);
    }

    @PostMapping("/{id}/run")
    public ScrapingJobDTO run(@PathVariable Long id) {
        return scrapingJobService.runJob(id);
    }
}
