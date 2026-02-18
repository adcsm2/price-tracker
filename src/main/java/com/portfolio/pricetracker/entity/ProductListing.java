package com.portfolio.pricetracker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_listings", uniqueConstraints = @UniqueConstraint(
        columnNames = {"product_id", "source_id"}
))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private WebsiteSource source;

    private String externalId;

    @Column(nullable = false)
    private String url;

    private BigDecimal currentPrice;

    @Builder.Default
    private String currency = "EUR";

    private Boolean inStock;

    private LocalDateTime lastScrapedAt;

    @OneToMany(mappedBy = "listing")
    @Builder.Default
    private List<PriceHistory> priceHistory = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
