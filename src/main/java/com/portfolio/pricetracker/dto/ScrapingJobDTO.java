package com.portfolio.pricetracker.dto;

import com.portfolio.pricetracker.entity.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrapingJobDTO {

    private Long id;
    private Long sourceId;
    private String sourceName;
    private String searchKeyword;
    private String category;
    private JobStatus status;
    private Integer itemsFound;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
