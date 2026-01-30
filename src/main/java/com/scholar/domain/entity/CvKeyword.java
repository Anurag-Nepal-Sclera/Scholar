package com.scholar.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CvKeyword entity representing extracted keywords from a CV.
 * Stores both original and normalized forms with weighting.
 */
@Entity
@Table(name = "cv_keyword", 
    indexes = {
        @Index(name = "idx_cv_keyword_tenant", columnList = "tenant_id"),
        @Index(name = "idx_cv_keyword_cv", columnList = "cv_id"),
        @Index(name = "idx_cv_keyword_normalized", columnList = "normalized_keyword"),
        @Index(name = "idx_cv_keyword_weight", columnList = "weight")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_cv_keyword_cv_normalized", columnNames = {"cv_id", "normalized_keyword"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CvKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cv_id", nullable = false)
    private CV cv;

    @Column(nullable = false)
    private String keyword;

    @Column(name = "normalized_keyword", nullable = false)
    private String normalizedKeyword;

    @Column(nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal weight = BigDecimal.ONE;

    @Column(nullable = false)
    @Builder.Default
    private Integer frequency = 1;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
