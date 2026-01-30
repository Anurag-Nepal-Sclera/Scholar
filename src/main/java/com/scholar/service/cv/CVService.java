package com.scholar.service.cv;

import com.scholar.domain.entity.CV;
import com.scholar.domain.entity.CvKeyword;
import com.scholar.domain.entity.Tenant;
import com.scholar.domain.entity.UserProfile;
import com.scholar.domain.repository.CVRepository;
import com.scholar.domain.repository.CvKeywordRepository;
import com.scholar.domain.repository.MatchResultRepository;
import com.scholar.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.scholar.service.matching.MatchingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing CV uploads, parsing, and keyword extraction.
 * Handles secure file storage and asynchronous text processing.
 */
@Service
@Slf4j
public class CVService {

    private final CVRepository cvRepository;
    private final CvKeywordRepository cvKeywordRepository;
    private final MatchResultRepository matchResultRepository;
    private final FileStorageService fileStorageService;
    private final DocumentTextExtractor textExtractor;
    private final OpenRouterService openRouterService;
    private final MatchingService matchingService;
    private final CVService self;

    public CVService(CVRepository cvRepository, 
                     CvKeywordRepository cvKeywordRepository,
                     MatchResultRepository matchResultRepository,
                     FileStorageService fileStorageService,
                     DocumentTextExtractor textExtractor,
                     OpenRouterService openRouterService,
                     MatchingService matchingService,
                     @Lazy CVService self) {
        this.cvRepository = cvRepository;
        this.cvKeywordRepository = cvKeywordRepository;
        this.matchResultRepository = matchResultRepository;
        this.fileStorageService = fileStorageService;
        this.textExtractor = textExtractor;
        this.openRouterService = openRouterService;
        this.matchingService = matchingService;
        this.self = self;
    }

    @Value("${scholar.cv.allowed-types}")
    private String allowedTypes;

    @Value("${scholar.cv.max-size-mb}")
    private long maxSizeMb;

    /**
     * Uploads and stores a CV file.
     * 
     * @param file the uploaded file
     * @param tenant the tenant
     * @param userProfile the user profile
     * @return the created CV entity
     */
    @Transactional
    public CV uploadCV(MultipartFile file, Tenant tenant, UserProfile userProfile) {
        // Validate file
        validateFile(file);

        try {
            // Store file
            String filePath = fileStorageService.storeFile(file, tenant.getId());

            // Create CV entity
            CV cv = CV.builder()
                .tenant(tenant)
                .userProfile(userProfile)
                .originalFilename(file.getOriginalFilename())
                .storedFilename(filePath.substring(filePath.lastIndexOf('/') + 1))
                .filePath(filePath)
                .fileSizeBytes(file.getSize())
                .mimeType(file.getContentType())
                .parsingStatus(CV.ParsingStatus.PENDING)
                .build();

            CV savedCV = cvRepository.save(cv);
            log.info("CV uploaded successfully: {} for tenant: {}", savedCV.getId(), tenant.getId());

            // Trigger async parsing after transaction commit to avoid race condition
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        self.parseAndExtractKeywords(savedCV.getId());
                    }
                });
            } else {
                self.parseAndExtractKeywords(savedCV.getId());
            }

            return savedCV;
        } catch (Exception e) {
            log.error("Failed to upload CV", e);
            throw new RuntimeException("CV upload failed", e);
        }
    }

    /**
     * Asynchronously parses CV text and extracts keywords.
     * 
     * @param cvId the CV identifier
     */
    @Async
    @Transactional
    public void parseAndExtractKeywords(UUID cvId) {
        log.info("Starting asynchronous parsing for CV ID: {}", cvId);
        try {
            CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new IllegalArgumentException("CV not found: " + cvId));

            log.debug("CV details retrieved: {}, MIME type: {}", cv.getOriginalFilename(), cv.getMimeType());

            // Update status to IN_PROGRESS
            cv.setParsingStatus(CV.ParsingStatus.IN_PROGRESS);
            cvRepository.save(cv);
            log.debug("CV status updated to IN_PROGRESS for ID: {}", cvId);

            // Extract text
            log.debug("Retrieving file from storage: {}", cv.getFilePath());
            byte[] fileBytes = fileStorageService.retrieveFile(cv.getFilePath());
            
            log.debug("Extracting text from document... size: {} bytes", fileBytes.length);
            String extractedText = textExtractor.extractText(fileBytes, cv.getMimeType());
            log.debug("Text extraction completed. Extracted length: {} characters", extractedText.length());

            // Extract keywords using AI (Comprehensive technical extraction)
            log.debug("Extracting ~200 technical keywords from text using AI...");
            List<String> aiKeywordsRaw = openRouterService.extractKeywords(extractedText);
            
            // Deduplicate keywords while preserving rank
            List<String> aiKeywords = new ArrayList<>(new java.util.LinkedHashSet<>(aiKeywordsRaw));
            log.debug("Deduplicated to {} unique technical keywords", aiKeywords.size());

            // Clear existing keywords for this CV to avoid unique constraint violations
            log.debug("Clearing existing keywords for CV ID: {}", cvId);
            cvKeywordRepository.deleteByCvId(cvId);
            // Flush to ensure deletions are executed before subsequent insertions
            cvKeywordRepository.flush();

            // Assign weights based on position (rank-based weighting)
            log.debug("Applying weight assignment to {} keywords", aiKeywords.size());
            List<CvKeyword> keywords = new ArrayList<>();
            for (int i = 0; i < aiKeywords.size(); i++) {
                String kw = aiKeywords.get(i);
                String normalized = kw.toLowerCase().trim();
                
                // Simple rank-based weight: first keywords (most significant) get higher weight
                // From 1.0 (rank 1) down to 0.1 (rank N)
                double weightValue = 1.0 - (0.9 * ((double) i / Math.max(1, aiKeywords.size() - 1)));
                
                keywords.add(CvKeyword.builder()
                    .tenant(cv.getTenant())
                    .cv(cv)
                    .keyword(kw)
                    .normalizedKeyword(normalized)
                    .weight(BigDecimal.valueOf(weightValue).setScale(4, RoundingMode.HALF_UP))
                    .frequency(1)
                    .build());
            }

            log.debug("Saving {} technical keywords to database", keywords.size());
            cvKeywordRepository.saveAllAndFlush(keywords);

            // Update CV status
            cv.setParsingStatus(CV.ParsingStatus.COMPLETED);
            cv.setParsedAt(LocalDateTime.now());
            cvRepository.save(cv);

            log.info("CV parsing completed successfully: {}. Total keywords saved: {}", cvId, keywords.size());

            // Automatically trigger match computation after successful parsing
            log.info("Triggering automatic match computation for CV: {}", cvId);
            matchingService.computeMatches(cvId, cv.getTenant().getId());
        } catch (Exception e) {
            log.error("CV parsing failed for ID: {}. Error: {}", cvId, e.getMessage(), e);
            cvRepository.findById(cvId).ifPresent(cv -> {
                cv.setParsingStatus(CV.ParsingStatus.FAILED);
                cvRepository.save(cv);
                log.debug("CV status updated to FAILED for ID: {}", cvId);
            });
        }
    }

    /**
     * Retrieves a CV by ID and tenant.
     * 
     * @param cvId the CV identifier
     * @param tenantId the tenant identifier
     * @return the CV entity
     */
    @Transactional(readOnly = true)
    public CV getCV(UUID cvId, UUID tenantId) {
        log.debug("Retrieving CV ID: {} for Tenant ID: {}", cvId, tenantId);
        return cvRepository.findByIdAndTenantId(cvId, tenantId)
            .orElseThrow(() -> {
                log.warn("CV ID: {} not found or access denied for Tenant ID: {}", cvId, tenantId);
                return new IllegalArgumentException("CV not found or access denied");
            });
    }

    /**
     * Retrieves all CVs for a tenant.
     * 
     * @param tenantId the tenant identifier
     * @param pageable pagination parameters
     * @return page of CV entities
     */
    @Transactional(readOnly = true)
    public Page<CV> getAllCVs(UUID tenantId, Pageable pageable) {
        log.debug("Listing all CVs for Tenant ID: {}, Page: {}, Size: {}", tenantId, pageable.getPageNumber(), pageable.getPageSize());
        return cvRepository.findAllByTenantId(tenantId, pageable);
    }

    /**
     * Retrieves all CVs for a user profile.
     * 
     * @param userProfileId the user profile identifier
     * @param tenantId the tenant identifier
     * @param pageable pagination parameters
     * @return page of CV entities
     */
    @Transactional(readOnly = true)
    public Page<CV> getCVsByUserProfile(UUID userProfileId, UUID tenantId, Pageable pageable) {
        log.debug("Listing CVs for User: {} in Tenant: {}", userProfileId, tenantId);
        return cvRepository.findByUserProfileIdAndTenantId(userProfileId, tenantId, pageable);
    }

    /**
     * Retrieves keywords for a CV.
     * 
     * @param cvId the CV identifier
     * @param tenantId the tenant identifier
     * @return list of keywords
     */
    @Transactional(readOnly = true)
    public List<CvKeyword> getCVKeywords(UUID cvId, UUID tenantId) {
        log.debug("Retrieving keywords for CV ID: {} in Tenant ID: {}", cvId, tenantId);
        return cvKeywordRepository.findByCvIdAndTenantId(cvId, tenantId);
    }

    /**
     * Retrieves the keyword count for a CV.
     * 
     * @param cvId the CV identifier
     * @return the count of keywords
     */
    @Transactional(readOnly = true)
    public int getKeywordCount(UUID cvId) {
        log.debug("Retrieving keyword count for CV ID: {}", cvId);
        return cvKeywordRepository.findByCvId(cvId).size();
    }

    /**
     * Deletes a CV and its associated data.
     * 
     * @param cvId the CV identifier
     * @param tenantId the tenant identifier
     */
    @Transactional
    public void deleteCV(UUID cvId, UUID tenantId) {
        log.info("Deleting CV ID: {} for Tenant ID: {}", cvId, tenantId);
        CV cv = getCV(cvId, tenantId);
        
        try {
            log.debug("Deleting file from storage: {}", cv.getFilePath());
            fileStorageService.deleteFile(cv.getFilePath());
        } catch (Exception e) {
            log.warn("Failed to delete CV file from storage: {}. Error: {}", cv.getFilePath(), e.getMessage());
        }

        // Manually delete related data since we removed cascade
        log.debug("Deleting keywords and match results for CV ID: {}", cvId);
        cvKeywordRepository.deleteByCvId(cvId);
        matchResultRepository.deleteByCvId(cvId);

        cvRepository.delete(cv);
        log.info("CV ID: {} successfully deleted from database", cvId);
    }

    /**
     * Validates uploaded file.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (!fileStorageService.isValidFileType(file, allowedTypes)) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: " + allowedTypes);
        }

        long maxSizeBytes = maxSizeMb * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("File size exceeds maximum allowed: " + maxSizeMb + "MB");
        }
    }
}
