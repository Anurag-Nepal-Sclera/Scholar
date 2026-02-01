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
 * Professor entity representing an academic researcher.
 * Associated with a university and contains research keywords.
 */
@Entity
@Table(name = "professor", 
    indexes = {
        @Index(name = "idx_professor_university", columnList = "university_id"),
        @Index(name = "idx_professor_email", columnList = "email"),
        @Index(name = "idx_professor_name", columnList = "last_name, first_name"),
        @Index(name = "idx_professor_department", columnList = "department"),
        @Index(name = "idx_professor_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Professor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 255)
    private String department;

    @Column(name = "research_area", columnDefinition = "TEXT")
    private String researchArea;

    @Column(name = "publications", columnDefinition = "TEXT")
    private String publications; // Comma or newline separated list of recent paper titles

    @Column(name = "profile_url", length = 1000)
    private String profileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private ProfessorStatus status = ProfessorStatus.ACTIVE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "professor", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<MatchResult> matchResults = new HashSet<>();

    public enum ProfessorStatus {
        ACTIVE, INACTIVE, DELETED
    }
}
