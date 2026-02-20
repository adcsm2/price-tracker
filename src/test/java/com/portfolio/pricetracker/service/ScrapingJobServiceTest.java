package com.portfolio.pricetracker.service;

import com.portfolio.pricetracker.dto.CreateScrapingJobRequest;
import com.portfolio.pricetracker.dto.ScrapedProductDTO;
import com.portfolio.pricetracker.dto.ScrapingJobDTO;
import com.portfolio.pricetracker.entity.*;
import com.portfolio.pricetracker.repository.ScrapingJobRepository;
import com.portfolio.pricetracker.repository.WebsiteSourceRepository;
import com.portfolio.pricetracker.service.scraper.ScraperFactory;
import com.portfolio.pricetracker.service.scraper.SiteScraper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScrapingJobServiceTest {

    @Mock
    private ScrapingJobRepository jobRepository;

    @Mock
    private WebsiteSourceRepository sourceRepository;

    @Mock
    private ScraperFactory scraperFactory;

    @Mock
    private SiteScraper siteScraper;

    @Mock
    private ProductUnificationService productUnificationService;

    @InjectMocks
    private ScrapingJobService service;

    private WebsiteSource amazonSource;

    @BeforeEach
    void setUp() {
        amazonSource = WebsiteSource.builder()
                .id(1L)
                .name("Amazon ES")
                .baseUrl("https://www.amazon.es")
                .scraperType(ScraperType.AMAZON)
                .status(SourceStatus.ACTIVE)
                .build();
    }

    @Test
    void should_CreateJob_Successfully() {
        CreateScrapingJobRequest request = new CreateScrapingJobRequest(1L, "rtx 4070", "gpu");

        when(sourceRepository.findById(1L)).thenReturn(Optional.of(amazonSource));
        when(jobRepository.save(any())).thenAnswer(inv -> {
            ScrapingJob job = inv.getArgument(0);
            job.setId(10L);
            return job;
        });

        ScrapingJobDTO result = service.createJob(request);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getSourceId()).isEqualTo(1L);
        assertThat(result.getSourceName()).isEqualTo("Amazon ES");
        assertThat(result.getSearchKeyword()).isEqualTo("rtx 4070");
        assertThat(result.getCategory()).isEqualTo("gpu");
        assertThat(result.getStatus()).isEqualTo(JobStatus.PENDING);
    }

    @Test
    void should_ThrowException_When_SourceNotFound() {
        CreateScrapingJobRequest request = new CreateScrapingJobRequest(99L, "rtx 4070", null);
        when(sourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createJob(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void should_RunJob_And_SetCompleted_When_ScraperSucceeds() {
        ScrapingJob job = ScrapingJob.builder()
                .id(1L)
                .source(amazonSource)
                .searchKeyword("rtx 4070")
                .status(JobStatus.PENDING)
                .build();

        List<ScrapedProductDTO> products = List.of(
                ScrapedProductDTO.builder().name("RTX 4070").price(new BigDecimal("599")).build(),
                ScrapedProductDTO.builder().name("RTX 4070 SUPER").price(new BigDecimal("649")).build()
        );

        when(jobRepository.findByIdWithSource(1L)).thenReturn(Optional.of(job));
        when(scraperFactory.getScraper(ScraperType.AMAZON)).thenReturn(siteScraper);
        when(siteScraper.scrape("rtx 4070", null)).thenReturn(products);
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScrapingJobDTO result = service.runJob(1L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(result.getItemsFound()).isEqualTo(2);
        assertThat(result.getStartedAt()).isNotNull();
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void should_RunJob_And_SetFailed_When_ScraperThrows() {
        ScrapingJob job = ScrapingJob.builder()
                .id(1L)
                .source(amazonSource)
                .searchKeyword("rtx 4070")
                .status(JobStatus.PENDING)
                .build();

        when(jobRepository.findByIdWithSource(1L)).thenReturn(Optional.of(job));
        when(scraperFactory.getScraper(ScraperType.AMAZON)).thenReturn(siteScraper);
        when(siteScraper.scrape(any(), any())).thenThrow(new RuntimeException("Network error"));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScrapingJobDTO result = service.runJob(1L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(result.getErrorMessage()).isEqualTo("Network error");
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getItemsFound()).isNull();
    }

    @Test
    void should_ThrowException_When_JobNotPending() {
        ScrapingJob job = ScrapingJob.builder()
                .id(1L)
                .source(amazonSource)
                .searchKeyword("rtx 4070")
                .status(JobStatus.RUNNING)
                .build();

        when(jobRepository.findByIdWithSource(1L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.runJob(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RUNNING");
    }

    @Test
    void should_ThrowException_When_JobNotFound() {
        when(jobRepository.findByIdWithSource(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.runJob(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void should_FindAll_ReturnAllJobs() {
        ScrapingJob job1 = ScrapingJob.builder().id(1L).source(amazonSource)
                .searchKeyword("rtx 4070").status(JobStatus.COMPLETED).build();
        ScrapingJob job2 = ScrapingJob.builder().id(2L).source(amazonSource)
                .searchKeyword("rtx 4080").status(JobStatus.PENDING).build();

        when(jobRepository.findAll()).thenReturn(List.of(job1, job2));

        List<ScrapingJobDTO> result = service.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ScrapingJobDTO::getId).containsExactly(1L, 2L);
    }

    @Test
    void should_FindById_ReturnJob() {
        ScrapingJob job = ScrapingJob.builder().id(1L).source(amazonSource)
                .searchKeyword("rtx 4070").status(JobStatus.PENDING).build();

        when(jobRepository.findByIdWithSource(1L)).thenReturn(Optional.of(job));

        ScrapingJobDTO result = service.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(JobStatus.PENDING);
    }

    @Test
    void should_RunPendingJobs_RunsEachPendingJob() {
        ScrapingJob job1 = ScrapingJob.builder().id(1L).source(amazonSource)
                .searchKeyword("rtx 4070").status(JobStatus.PENDING).build();
        ScrapingJob job2 = ScrapingJob.builder().id(2L).source(amazonSource)
                .searchKeyword("rtx 4080").status(JobStatus.PENDING).build();

        when(jobRepository.findByStatus(JobStatus.PENDING)).thenReturn(List.of(job1, job2));
        when(jobRepository.findByIdWithSource(1L)).thenReturn(Optional.of(job1));
        when(jobRepository.findByIdWithSource(2L)).thenReturn(Optional.of(job2));
        when(scraperFactory.getScraper(any())).thenReturn(siteScraper);
        when(siteScraper.scrape(any(), any())).thenReturn(List.of());
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.runPendingJobs();

        verify(jobRepository, times(2)).findByIdWithSource(anyLong());
        verify(siteScraper, times(2)).scrape(any(), any());
    }
}
