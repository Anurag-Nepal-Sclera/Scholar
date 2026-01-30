package com.scholar.domain.repository;

import com.scholar.domain.entity.University;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for University entity operations.
 */
@Repository
public interface UniversityRepository extends JpaRepository<University, UUID> {
    
    Page<University> findByStatus(University.UniversityStatus status, Pageable pageable);
    
    @Query("SELECT u FROM University u WHERE u.country = :country AND u.status = 'ACTIVE'")
    List<University> findActiveByCountry(@Param("country") String country);
    
    @Query("SELECT u FROM University u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<University> searchByName(@Param("name") String name);
}
