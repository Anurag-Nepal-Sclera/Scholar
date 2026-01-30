package com.scholar.domain.repository;

import com.scholar.domain.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Tenant-safe repository for UserProfile entity operations.
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    
    Optional<UserProfile> findByEmail(String email);
}
