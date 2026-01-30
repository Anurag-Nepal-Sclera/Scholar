package com.scholar.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * EmailCampaign entity representing an email outreach campaign.
 * Manages batch email sending to matched professors.
 */
@Entity
@Table(name = "email_campaign", 
    indexes = {
        @Index(name = "idx_email_campaign_tenant", columnList = "tenant_id"),
        @Index(name = "idx_email_campaign_cv", columnList = "cv_id"),
        @Index(name = "idx_email_campaign_status", columnList = "status"),
        @Index(name = "idx_email_campaign_scheduled", columnList = "scheduled_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailCampaign {

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
    @JoinColumn(name = "smtp_account_id", nullable = false)
    private SmtpAccount smtpAccount;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    @Column(name = "min_match_score", nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal minMatchScore = new BigDecimal("0.5000");

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(name = "total_recipients", nullable = false)
    @Builder.Default
    private Integer totalRecipients = 0;

    @Column(name = "sent_count", nullable = false)
    @Builder.Default
    private Integer sentCount = 0;

    @Column(name = "failed_count", nullable = false)
    @Builder.Default
    private Integer failedCount = 0;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "emailCampaign", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<EmailLog> emailLogs = new HashSet<>();

    public enum CampaignStatus {
        DRAFT, SCHEDULED, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
    }
}
