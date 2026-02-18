package com.portfolio.pricetracker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "website_sources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebsiteSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String baseUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScraperType scraperType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceStatus status;

    private LocalDateTime lastScrapedAt;

    @Builder.Default
    private Integer successfulScrapes = 0;

    @Builder.Default
    private Integer failedScrapes = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
