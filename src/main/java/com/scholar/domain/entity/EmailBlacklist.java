package com.scholar.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * EmailBlacklist entity for preventing emails to certain addresses.
 * Can be tenant-specific or global (tenant_id = null).
 */
@Entity
@Table(name = "email_blacklist", 
    indexes = {
        @Index(name = "idx_email_blacklist_tenant", columnList = "tenant_id"),
        @Index(name = "idx_email_blacklist_email", columnList = "email")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(nullable = false)
    private String email;

    @Column(length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = "blacklisted_at", nullable = false, updatable = false)
    private LocalDateTime blacklistedAt;
}
