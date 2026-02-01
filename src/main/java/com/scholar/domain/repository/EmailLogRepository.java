package com.scholar.domain.repository;

import com.scholar.domain.entity.EmailLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Tenant-safe repository for EmailLog entity operations.
 */
@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, UUID> {
    
    @Query("SELECT e FROM EmailLog e " +
           "JOIN FETCH e.professor p " +
           "WHERE e.emailCampaign.id = :campaignId")
    Page<EmailLog> findByCampaignId(@Param("campaignId") UUID campaignId, Pageable pageable);
    
    @Query("SELECT e FROM EmailLog e " +
           "JOIN FETCH e.professor p " +
           "WHERE e.emailCampaign.id = :campaignId AND e.status = :status")
    List<EmailLog> findByCampaignIdAndStatus(@Param("campaignId") UUID campaignId, 
                                             @Param("status") EmailLog.EmailStatus status);
    
    @Query("SELECT COUNT(e) FROM EmailLog e WHERE e.emailCampaign.id = :campaignId AND e.status = :status")
    long countByCampaignIdAndStatus(@Param("campaignId") UUID campaignId, 
                                    @Param("status") EmailLog.EmailStatus status);

    @Query("SELECT COUNT(e) FROM EmailLog e WHERE e.tenant.id = :tenantId AND e.status = :status")
    long countByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") EmailLog.EmailStatus status);

    boolean existsByEmailCampaignIdAndProfessorId(UUID emailCampaignId, UUID professorId);
}
