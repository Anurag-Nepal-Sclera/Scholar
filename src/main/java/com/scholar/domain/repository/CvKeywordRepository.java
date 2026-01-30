package com.scholar.domain.repository;

import com.scholar.domain.entity.CvKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Tenant-safe repository for CvKeyword entity operations.
 */
@Repository
public interface CvKeywordRepository extends JpaRepository<CvKeyword, UUID> {
    
    @Query("SELECT k FROM CvKeyword k WHERE k.cv.id = :cvId AND k.tenant.id = :tenantId")
    List<CvKeyword> findByCvIdAndTenantId(@Param("cvId") UUID cvId, @Param("tenantId") UUID tenantId);
    
    @Query("SELECT k FROM CvKeyword k WHERE k.cv.id = :cvId")
    List<CvKeyword> findByCvId(@Param("cvId") UUID cvId);
    
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM CvKeyword k WHERE k.cv.id = :cvId")
    void deleteByCvId(@Param("cvId") UUID cvId);

    void flush();

    <S extends CvKeyword> List<S> saveAllAndFlush(Iterable<S> entities);
}
