package com.scholar.domain.repository;

import com.scholar.domain.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Tenant entity operations.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    
    Optional<Tenant> findByEmail(String email);
    
    boolean existsByEmail(String email);

    List<Tenant> findByOwnerId(UUID ownerId);

    Optional<Tenant> findByIdAndOwnerId(UUID id, UUID ownerId);
}
