package com.scholar.domain.repository;

import com.scholar.domain.entity.CV;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Tenant-safe repository for CV entity operations.
 */
@Repository
public interface CVRepository extends JpaRepository<CV, UUID> {
    
    @Query("SELECT c FROM CV c WHERE c.tenant.id = :tenantId")
    Page<CV> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);
    
    @Query("SELECT c FROM CV c WHERE c.tenant.id = :tenantId AND c.id = :id")
    Optional<CV> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
    
    @Query("SELECT c FROM CV c WHERE c.tenant.id = :tenantId AND c.userProfile.id = :userProfileId")
    Page<CV> findByUserProfileIdAndTenantId(@Param("userProfileId") UUID userProfileId, 
                                            @Param("tenantId") UUID tenantId, 
                                            Pageable pageable);
    
    @Query("SELECT c FROM CV c WHERE c.parsingStatus = :status")
    List<CV> findByParsingStatus(@Param("status") CV.ParsingStatus status);

    @Query("SELECT COUNT(c) FROM CV c WHERE c.tenant.id = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);
}
