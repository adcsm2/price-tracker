package com.portfolio.pricetracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateScrapingJobRequest {

    @NotNull
    private Long sourceId;

    @NotBlank
    private String keyword;

    private String category;
}
