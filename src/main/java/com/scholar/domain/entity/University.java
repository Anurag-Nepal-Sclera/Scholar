package com.scholar.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * University entity representing an academic institution.
 */
@Entity
@Table(name = "university", 
    indexes = {
        @Index(name = "idx_university_name", columnList = "name"),
        @Index(name = "idx_university_country", columnList = "country"),
        @Index(name = "idx_university_rank", columnList = "rank_global"),
        @Index(name = "idx_university_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class University {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(length = 500)
    private String website;

    @Column(name = "rank_global")
    private Integer rankGlobal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private UniversityStatus status = UniversityStatus.ACTIVE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "university", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Professor> professors = new HashSet<>();

    public enum UniversityStatus {
        ACTIVE, INACTIVE, DELETED
    }
}
