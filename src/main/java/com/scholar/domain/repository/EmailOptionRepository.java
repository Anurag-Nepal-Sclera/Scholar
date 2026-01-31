package com.scholar.domain.repository;

import com.scholar.domain.entity.EmailOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for EmailOption entity operations.
 */
@Repository
public interface EmailOptionRepository extends JpaRepository<EmailOption, UUID> {
    List<EmailOption> findByEmailLogId(UUID emailLogId);
}
