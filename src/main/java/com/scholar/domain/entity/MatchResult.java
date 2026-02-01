package com.scholar.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * MatchResult entity representing the computed match between a CV and a professor.
 * Contains match score, matched keywords, and keyword statistics.
 */
@Entity
@Table(name = "match_result", 
    indexes = {
        @Index(name = "idx_match_result_tenant", columnList = "tenant_id"),
        @Index(name = "idx_match_result_cv", columnList = "cv_id"),
        @Index(name = "idx_match_result_professor", columnList = "professor_id"),
        @Index(name = "idx_match_result_score", columnList = "match_score"),
        @Index(name = "idx_match_result_computed_at", columnList = "computed_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_match_result_cv_professor", columnNames = {"cv_id", "professor_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cv_id", nullable = false)
    private CV cv;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @Column(name = "match_score", nullable = false, precision = 8, scale = 6)
    private BigDecimal matchScore;

    @Column(name = "matched_keywords", columnDefinition = "TEXT")
    private String matchedKeywords;

    @Column(name = "total_cv_keywords", nullable = false)
    private Integer totalCvKeywords;

    @Column(name = "total_professor_keywords", nullable = false)
    private Integer totalProfessorKeywords;

    @Column(name = "total_matched_keywords", nullable = false)
    private Integer totalMatchedKeywords;

    @CreationTimestamp
    @Column(name = "computed_at", nullable = false, updatable = false)
    private LocalDateTime computedAt;

}
