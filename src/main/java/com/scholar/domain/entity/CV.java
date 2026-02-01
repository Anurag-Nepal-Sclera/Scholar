package com.scholar.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * CV entity representing an uploaded curriculum vitae.
 * Maintains parsing status and extracted text.
 */
@Entity
@Table(name = "cv", 
    indexes = {
        @Index(name = "idx_cv_tenant", columnList = "tenant_id"),
        @Index(name = "idx_cv_user_profile", columnList = "user_profile_id"),
        @Index(name = "idx_cv_parsing_status", columnList = "parsing_status"),
        @Index(name = "idx_cv_uploaded_at", columnList = "uploaded_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CV {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, length = 500)
    private String storedFilename;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "parsing_status", nullable = false, length = 50)
    @Builder.Default
    private ParsingStatus parsingStatus = ParsingStatus.PENDING;

    @Column(name = "parsed_at")
    private LocalDateTime parsedAt;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @OneToMany(mappedBy = "cv", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CvExperience> experiences = new HashSet<>();

    public enum ParsingStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED
    }
}
