package com.scholar.service.matching;

import com.scholar.domain.entity.*;
import com.scholar.domain.repository.*;
import com.scholar.dto.response.EmailOptionResponse;
import com.scholar.dto.response.MatchResultResponse;
import com.scholar.service.email.EmailCampaignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for matching CVs with professors based on keyword similarity.
 * Implements deterministic weighted scoring algorithm.
 */
@Service
@Slf4j
public class MatchingService {

    private final CVRepository cvRepository;
    private final CvKeywordRepository cvKeywordRepository;
    private final ProfessorRepository professorRepository;
    private final MatchResultRepository matchResultRepository;
    private final EmailLogRepository emailLogRepository;
    private final EmailCampaignService emailCampaignService;
    private final MatchingService self;

    public MatchingService(CVRepository cvRepository,
                           CvKeywordRepository cvKeywordRepository,
                           ProfessorRepository professorRepository,
                           MatchResultRepository matchResultRepository,
                           EmailLogRepository emailLogRepository,
                           EmailCampaignService emailCampaignService,
                           @Lazy MatchingService self) {
        this.cvRepository = cvRepository;
        this.cvKeywordRepository = cvKeywordRepository;
        this.professorRepository = professorRepository;
        this.matchResultRepository = matchResultRepository;
        this.emailLogRepository = emailLogRepository;
        this.emailCampaignService = emailCampaignService;
        this.self = self;
    }

    /**
     * Computes matches for a CV against all active professors.
     *
     * @param cvId the CV identifier
     * @param tenantId the tenant identifier
     */
    @Async
    @Transactional
    public void computeMatches(UUID cvId, UUID tenantId) {
        log.info("Starting match computation for CV ID: {} in Tenant ID: {}", cvId, tenantId);
        try {
            CV cv = cvRepository.findByIdAndTenantId(cvId, tenantId)
                    .orElseThrow(() -> {
                        log.error("CV not found or access denied: {} for tenant: {}", cvId, tenantId);
                        return new IllegalArgumentException("CV not found: " + cvId);
                    });

            if (cv.getParsingStatus() != CV.ParsingStatus.COMPLETED) {
                log.warn("Computation skipped: CV {} status is {}, expected COMPLETED", cvId, cv.getParsingStatus());
                return;
            }

            // Get CV keywords
            log.debug("Fetching keywords for CV ID: {}", cvId);
            List<CvKeyword> cvKeywords = cvKeywordRepository.findByCvId(cvId);
            if (cvKeywords.isEmpty()) {
                log.warn("Computation aborted: No keywords found for CV ID: {}", cvId);
                return;
            }

            log.debug("Preparing keyword map for {} CV keywords", cvKeywords.size());
            Map<String, BigDecimal> cvKeywordMap = cvKeywords.stream()
                    .collect(Collectors.toMap(
                            CvKeyword::getNormalizedKeyword,
                            CvKeyword::getWeight,
                            (w1, w2) -> w1.max(w2)
                    ));

            // Get all active professors
            log.debug("Fetching active professors for matching...");
            List<Professor> professors = professorRepository.findAll().stream()
                    .filter(p -> p.getStatus() == Professor.ProfessorStatus.ACTIVE)
                    .collect(Collectors.toList());

            log.info("Computing matches for CV {} against {} active professors", cvId, professors.size());

            // Get existing match results to perform upsert
            log.debug("Fetching existing match results for CV ID: {} to avoid duplicates", cvId);
            Map<UUID, MatchResult> existingMatchMap = matchResultRepository.findByCvId(cvId).stream()
                    .collect(Collectors.toMap(m -> m.getProfessor().getId(), m -> m));

            List<MatchResult> matchResultsToSave = new ArrayList<>();

            for (Professor professor : professors) {
                log.trace("Matching CV {} against Professor {} ({})", cvId, professor.getId(), professor.getLastName());
                MatchResult existingMatch = existingMatchMap.get(professor.getId());
                MatchResult matchResult = computeSingleMatch(cv, professor, cvKeywordMap, existingMatch);
                if (matchResult != null) {
                    matchResultsToSave.add(matchResult);
                }
            }

            // Save all match results (upsert)
            log.debug("Saving {} match results (upsert) for CV ID: {}", matchResultsToSave.size(), cvId);
            matchResultRepository.saveAll(matchResultsToSave);
            matchResultRepository.flush(); // Ensure matches are visible to subsequent queries in this transaction context
            log.info("Match computation completed for CV {}. Found {} total matches.", cvId, matchResultsToSave.size());

            // Phase 2: Disabled automatic campaign creation to allow human review layer
            // Campaigns are now created manually via the "Generate Emails" button in MatchesPage
            // if (!matchResultsToSave.isEmpty()) {
            //     log.info("Creating automatic AI outreach campaign for CV: {}", cvId);
            //     emailCampaignService.createAutoCampaign(cvId, tenantId);
            // }
            log.info("Match computation completed. Awaiting manual campaign creation via UI.");

        } catch (Exception e) {
            log.error("Failed to compute matches for CV ID: {}. Error: {}", cvId, e.getMessage(), e);
        }
    }

    /**
     * Computes match score between a CV and a single professor.
     * Performs an update if existingMatch is provided, otherwise creates a new one.
     */
    private MatchResult computeSingleMatch(CV cv, Professor professor, Map<String, BigDecimal> cvKeywordMap, MatchResult existingMatch) {
        String researchArea = professor.getResearchArea() != null ? professor.getResearchArea().toLowerCase() : "";
        String department = professor.getDepartment() != null ? professor.getDepartment().toLowerCase() : "";
        String combinedText = researchArea + " " + department;

        if (combinedText.isBlank()) {
            return null;
        }

        Set<String> matchedKeywords = new HashSet<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal maxPossibleScore = BigDecimal.ZERO;

        // Count "professor keywords" by splitting research area (approximate for stats)
        String[] profKeywords = researchArea.split("[,;\\s]+");
        int profKeywordCount = (int) Arrays.stream(profKeywords).filter(s -> !s.isBlank()).distinct().count();

        for (Map.Entry<String, BigDecimal> entry : cvKeywordMap.entrySet()) {
            String keyword = entry.getKey();
            BigDecimal weight = entry.getValue();
            maxPossibleScore = maxPossibleScore.add(weight);

            if (combinedText.contains(keyword)) {
                matchedKeywords.add(keyword);
                totalScore = totalScore.add(weight);
            }
        }

        if (matchedKeywords.isEmpty()) {
            return null;
        }

        // Normalize score to [0, 1]
        BigDecimal matchScore = maxPossibleScore.compareTo(BigDecimal.ZERO) > 0
                ? totalScore.divide(maxPossibleScore, 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String matchedKeywordsStr = String.join(", ", matchedKeywords);

        if (existingMatch != null) {
            existingMatch.setMatchScore(matchScore);
            existingMatch.setMatchedKeywords(matchedKeywordsStr);
            existingMatch.setTotalCvKeywords(cvKeywordMap.size());
            existingMatch.setTotalProfessorKeywords(profKeywordCount);
            existingMatch.setTotalMatchedKeywords(matchedKeywords.size());
            return existingMatch;
        } else {
            return MatchResult.builder()
                    .tenant(cv.getTenant())
                    .cv(cv)
                    .professor(professor)
                    .matchScore(matchScore)
                    .matchedKeywords(matchedKeywordsStr)
                    .totalCvKeywords(cvKeywordMap.size())
                    .totalProfessorKeywords(profKeywordCount)
                    .totalMatchedKeywords(matchedKeywords.size())
                    .build();
        }
    }

    /**
     * Retrieves match results for a CV.
     *
     * @param cvId the CV identifier
     * @param tenantId the tenant identifier
     * @param pageable pagination parameters
     * @return page of match results ordered by score descending
     */
    /**
     * Retrieves match results for a CV as DTOs.
     *
     * @param cvId the CV identifier
     * @param tenantId the tenant identifier
     * @param pageable pagination parameters
     * @return page of match results response
     */
    @Transactional(readOnly = true)
    public Page<MatchResultResponse> getMatchResultsResponse(UUID cvId, UUID tenantId, Pageable pageable) {
        return matchResultRepository.findByCvIdAndTenantIdOrderByScoreDesc(cvId, tenantId, pageable)
                .map(this::toMatchResultResponse);
    }

    /**
     * Retrieves match results above a minimum score threshold as DTOs.
     *
     * @param cvId the CV identifier
     * @param tenantId the tenant identifier
     * @param minScore minimum match score
     * @return list of match results response
     */
    @Transactional(readOnly = true)
    public List<MatchResultResponse> getMatchResultsAboveThresholdResponse(UUID cvId, UUID tenantId, BigDecimal minScore) {
        return matchResultRepository.findByCvIdAndTenantIdAndMinScore(cvId, tenantId, minScore).stream()
                .map(this::toMatchResultResponse)
                .collect(Collectors.toList());
    }

    private MatchResultResponse toMatchResultResponse(MatchResult match) {
        List<EmailOptionResponse> options = null;
        EmailLog emailLog = emailLogRepository.findLatestByMatchResultId(match.getId()).orElse(null);

        if (emailLog != null && emailLog.getOptions() != null) {
            options = emailLog.getOptions().stream()
                    .map(o -> EmailOptionResponse.builder()
                            .id(o.getId())
                            .body(o.getBody())
                            .isSelected(o.getIsSelected())
                            .build())
                    .collect(Collectors.toList());
        }

        return MatchResultResponse.builder()
                .id(match.getId())
                .professor(MatchResultResponse.ProfessorSummary.builder()
                        .id(match.getProfessor().getId())
                        .firstName(match.getProfessor().getFirstName())
                        .lastName(match.getProfessor().getLastName())
                        .email(match.getProfessor().getEmail())
                        .department(match.getProfessor().getDepartment())
                        .universityName(match.getProfessor().getUniversity().getName())
                        .universityCountry(match.getProfessor().getUniversity().getCountry())
                        .build())
                .matchScore(match.getMatchScore())
                .matchedKeywords(match.getMatchedKeywords())
                .totalCvKeywords(match.getTotalCvKeywords())
                .totalProfessorKeywords(match.getTotalProfessorKeywords())
                .totalMatchedKeywords(match.getTotalMatchedKeywords())
                .emailOptions(options)
                .build();
    }

    /**
     * Recomputes all matches for a CV (clears existing matches first).
     *
     * @param cvId the CV identifier
     * @param tenantId the tenant identifier
     */
    @Transactional
    public void recomputeMatches(UUID cvId, UUID tenantId) {
        log.info("Recomputing matches for CV: {}", cvId);
        matchResultRepository.deleteByCvId(cvId);

        // Trigger async computation after transaction commit to avoid race condition
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    self.computeMatches(cvId, tenantId);
                }
            });
        } else {
            self.computeMatches(cvId, tenantId);
        }
    }
}