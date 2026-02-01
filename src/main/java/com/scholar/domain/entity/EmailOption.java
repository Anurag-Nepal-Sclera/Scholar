package com.scholar.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * EmailOption entity representing one of multiple AI-generated email templates for a match.
 */
@Entity
@Table(name = "email_option",
    indexes = {
        @Index(name = "idx_email_option_log", columnList = "email_log_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "email_log_id", nullable = false)
    private EmailLog emailLog;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "is_selected")
    @Builder.Default
    private Boolean isSelected = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
