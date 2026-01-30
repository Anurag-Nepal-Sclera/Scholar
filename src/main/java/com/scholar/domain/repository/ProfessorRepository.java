package com.scholar.domain.repository;

import com.scholar.domain.entity.Professor;
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
 * Repository for Professor entity operations.
 */
@Repository
public interface ProfessorRepository extends JpaRepository<Professor, UUID> {
    
    Optional<Professor> findByEmail(String email);
    
    @Query("SELECT p FROM Professor p WHERE p.university.id = :universityId AND p.status = 'ACTIVE'")
    Page<Professor> findActiveByUniversityId(@Param("universityId") UUID universityId, Pageable pageable);
    
    @Query("SELECT p FROM Professor p WHERE p.status = 'ACTIVE'")
    Page<Professor> findAllActive(Pageable pageable);
    
    @Query("SELECT p FROM Professor p WHERE p.department = :department AND p.status = 'ACTIVE'")
    List<Professor> findActiveByDepartment(@Param("department") String department);
}
