package com.scholar.domain.repository;

import com.scholar.domain.entity.EmailBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for EmailBlacklist entity operations.
 */
@Repository
public interface EmailBlacklistRepository extends JpaRepository<EmailBlacklist, UUID> {
    
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM EmailBlacklist b " +
           "WHERE b.email = :email AND (b.tenant.id = :tenantId OR b.tenant IS NULL)")
    boolean isBlacklisted(@Param("email") String email, @Param("tenantId") UUID tenantId);
    
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM EmailBlacklist b " +
           "WHERE b.email = :email AND b.tenant IS NULL")
    boolean isGloballyBlacklisted(@Param("email") String email);
}
