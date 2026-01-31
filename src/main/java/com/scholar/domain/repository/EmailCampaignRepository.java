package com.scholar.domain.repository;

import com.scholar.domain.entity.EmailCampaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Tenant-safe repository for EmailCampaign entity operations.
 */
@Repository
public interface EmailCampaignRepository extends JpaRepository<EmailCampaign, UUID> {
    
    @Query("SELECT c FROM EmailCampaign c WHERE c.tenant.id = :tenantId")
    Page<EmailCampaign> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);
    
    @Query("SELECT c FROM EmailCampaign c WHERE c.tenant.id = :tenantId AND c.id = :id")
    Optional<EmailCampaign> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
    
    @Query("SELECT c FROM EmailCampaign c WHERE c.status = :status AND c.scheduledAt <= :now")
    List<EmailCampaign> findScheduledCampaigns(@Param("status") EmailCampaign.CampaignStatus status, 
                                                @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE EmailCampaign c SET c.status = :newStatus, c.startedAt = :now WHERE c.id = :id AND c.status IN :allowedStatuses")
    int updateStatusIfAllowed(@Param("id") UUID id, 
                             @Param("newStatus") EmailCampaign.CampaignStatus newStatus, 
                             @Param("now") LocalDateTime now,
                             @Param("allowedStatuses") List<EmailCampaign.CampaignStatus> allowedStatuses);

    @Query("SELECT COUNT(c) FROM EmailCampaign c WHERE c.tenant.id = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);
}
