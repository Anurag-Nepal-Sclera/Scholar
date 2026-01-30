package com.scholar.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SmtpAccount entity representing email sending configuration for a tenant.
 * Passwords are encrypted at rest using AES encryption.
 */
@Entity
@Table(name = "smtp_account", 
    indexes = {
        @Index(name = "idx_smtp_account_tenant", columnList = "tenant_id"),
        @Index(name = "idx_smtp_account_status", columnList = "status")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_smtp_account_tenant", columnNames = {"tenant_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmtpAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private String email;

    @Column(name = "smtp_host", nullable = false)
    private String smtpHost;

    @Column(name = "smtp_port", nullable = false)
    private Integer smtpPort;

    @Column(nullable = false)
    private String username;

    @Column(name = "encrypted_password", nullable = false, columnDefinition = "TEXT")
    private String encryptedPassword;

    @Column(name = "use_tls", nullable = false)
    @Builder.Default
    private Boolean useTls = true;

    @Column(name = "use_ssl", nullable = false)
    @Builder.Default
    private Boolean useSsl = false;

    @Column(name = "from_name")
    private String fromName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private SmtpAccountStatus status = SmtpAccountStatus.ACTIVE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum SmtpAccountStatus {
        ACTIVE, INACTIVE, DELETED
    }
}
