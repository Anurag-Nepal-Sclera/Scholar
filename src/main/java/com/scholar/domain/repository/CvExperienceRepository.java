package com.scholar.domain.repository;

import com.scholar.domain.entity.CvExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CvExperienceRepository extends JpaRepository<CvExperience, UUID> {
    List<CvExperience> findByCvId(UUID cvId);
    void deleteByCvId(UUID cvId);
}
