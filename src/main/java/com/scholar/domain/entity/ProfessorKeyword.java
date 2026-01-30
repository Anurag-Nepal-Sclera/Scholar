package com.scholar.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ProfessorKeyword entity representing research keywords for a professor.
 * Extracted from various sources with different weights.
 */
@Entity
@Table(name = "professor_keyword", 
    indexes = {
        @Index(name = "idx_professor_keyword_professor", columnList = "professor_id"),
        @Index(name = "idx_professor_keyword_normalized", columnList = "normalized_keyword"),
        @Index(name = "idx_professor_keyword_weight", columnList = "weight")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_professor_keyword_professor_normalized", 
                         columnNames = {"professor_id", "normalized_keyword"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfessorKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @Column(nullable = false)
    private String keyword;

    @Column(name = "normalized_keyword", nullable = false)
    private String normalizedKeyword;

    @Column(nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal weight = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private KeywordSource source;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum KeywordSource {
        RESEARCH_AREA, PUBLICATION, MANUAL
    }
}
