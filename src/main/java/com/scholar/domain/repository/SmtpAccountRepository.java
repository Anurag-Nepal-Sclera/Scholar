package com.scholar.domain.repository;

import com.scholar.domain.entity.SmtpAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Tenant-safe repository for SmtpAccount entity operations.
 */
@Repository
public interface SmtpAccountRepository extends JpaRepository<SmtpAccount, UUID> {
    
    @Query("SELECT s FROM SmtpAccount s WHERE s.tenant.id = :tenantId")
    Optional<SmtpAccount> findByTenantId(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT s FROM SmtpAccount s WHERE s.tenant.id = :tenantId AND s.status = 'ACTIVE'")
    Optional<SmtpAccount> findActiveByTenantId(@Param("tenantId") UUID tenantId);
}
