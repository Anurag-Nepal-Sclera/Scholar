package com.scholar.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * EmailLog entity representing individual email send attempts within a campaign.
 * Tracks status, retries, and errors for audit and debugging.
 */
@Entity
@Table(name = "email_log", 
    indexes = {
        @Index(name = "idx_email_log_tenant", columnList = "tenant_id"),
        @Index(name = "idx_email_log_campaign", columnList = "email_campaign_id"),
        @Index(name = "idx_email_log_professor", columnList = "professor_id"),
        @Index(name = "idx_email_log_status", columnList = "status"),
        @Index(name = "idx_email_log_sent_at", columnList = "sent_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_email_log_campaign_professor", 
                         columnNames = {"email_campaign_id", "professor_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "email_campaign_id", nullable = false)
    private EmailCampaign emailCampaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_result_id", nullable = false)
    private MatchResult matchResult;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "alternate_bodies", columnDefinition = "TEXT")
    private String alternateBodies; // Store alternate options

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private EmailStatus status = EmailStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum EmailStatus {
        PENDING, SENDING, SENT, FAILED, BLACKLISTED
    }
}
