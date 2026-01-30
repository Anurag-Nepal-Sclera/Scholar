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
 * Tenant entity representing a multi-tenant organization.
 * Each tenant has complete data isolation for all owned resources.
 */
@Entity
@Table(name = "tenant", indexes = {
    @Index(name = "idx_tenant_email", columnList = "email"),
    @Index(name = "idx_tenant_status", columnList = "status"),
    @Index(name = "idx_tenant_owner", columnList = "owner_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserProfile owner;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private TenantStatus status = TenantStatus.ACTIVE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CV> cvs = new HashSet<>();

    @OneToOne(mappedBy = "tenant", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private SmtpAccount smtpAccount;

    public enum TenantStatus {
        ACTIVE, SUSPENDED, DELETED
    }
}
