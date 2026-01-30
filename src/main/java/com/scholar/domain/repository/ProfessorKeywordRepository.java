package com.scholar.domain.repository;

import com.scholar.domain.entity.ProfessorKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for ProfessorKeyword entity operations.
 */
@Repository
public interface ProfessorKeywordRepository extends JpaRepository<ProfessorKeyword, UUID> {
    
    @Query("SELECT k FROM ProfessorKeyword k WHERE k.professor.id = :professorId")
    List<ProfessorKeyword> findByProfessorId(@Param("professorId") UUID professorId);
    
    @Query("SELECT k FROM ProfessorKeyword k WHERE k.normalizedKeyword IN :keywords")
    List<ProfessorKeyword> findByNormalizedKeywords(@Param("keywords") List<String> keywords);
}
