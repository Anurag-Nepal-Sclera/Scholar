package com.scholar.domain.repository;

import com.scholar.domain.entity.MatchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Tenant-safe repository for MatchResult entity operations.
 */
@Repository
public interface MatchResultRepository extends JpaRepository<MatchResult, UUID> {
    
    @Query("SELECT m FROM MatchResult m " +
           "JOIN FETCH m.professor p " +
           "JOIN FETCH p.university u " +
           "WHERE m.tenant.id = :tenantId AND m.cv.id = :cvId " +
           "ORDER BY m.matchScore DESC")
    Page<MatchResult> findByCvIdAndTenantIdOrderByScoreDesc(@Param("cvId") UUID cvId, 
                                                              @Param("tenantId") UUID tenantId, 
                                                              Pageable pageable);
    
    @Query("SELECT m FROM MatchResult m " +
           "JOIN FETCH m.professor p " +
           "JOIN FETCH p.university u " +
           "WHERE m.tenant.id = :tenantId AND m.cv.id = :cvId AND m.matchScore >= :minScore " +
           "ORDER BY m.matchScore DESC")
    List<MatchResult> findByCvIdAndTenantIdAndMinScore(@Param("cvId") UUID cvId, 
                                                        @Param("tenantId") UUID tenantId, 
                                                        @Param("minScore") BigDecimal minScore);
    
    @Query("SELECT m FROM MatchResult m WHERE m.cv.id = :cvId AND m.professor.id = :professorId")
    Optional<MatchResult> findByCvIdAndProfessorId(@Param("cvId") UUID cvId, @Param("professorId") UUID professorId);
    
    @Query("SELECT m FROM MatchResult m WHERE m.cv.id = :cvId")
    List<MatchResult> findByCvId(@Param("cvId") UUID cvId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM MatchResult m WHERE m.cv.id = :cvId")
    void deleteByCvId(@Param("cvId") UUID cvId);

    @Query("SELECT COUNT(m) FROM MatchResult m WHERE m.tenant.id = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);
}
