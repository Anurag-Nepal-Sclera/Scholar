package com.scholar.controller;

import com.scholar.config.security.SecurityUtils;
import com.scholar.domain.entity.MatchResult;
import com.scholar.dto.response.ApiResponse;
import com.scholar.dto.response.MatchResultResponse;
import com.scholar.dto.response.EmailOptionResponse;
import com.scholar.service.matching.MatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for match result operations.
 */
@RestController
@RequestMapping("/v1/matches")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Match Results", description = "APIs for retrieving CV-Professor matches")
public class MatchController {

    private final MatchingService matchingService;
    private final SecurityUtils securityUtils;

    @GetMapping("/cv/{cvId}")
    @Operation(summary = "Get match results", description = "Retrieve all match results for a CV, ordered by score")
    public ResponseEntity<ApiResponse<Page<MatchResultResponse>>> getMatches(
        @PathVariable UUID cvId,
        @RequestParam UUID tenantId,
        @Parameter(hidden = true) Pageable pageable
    ) {
        try {
            securityUtils.validateTenantOwnership(tenantId);
            Page<MatchResult> matches = matchingService.getMatchResults(cvId, tenantId, pageable);
            Page<MatchResultResponse> response = matches.map(this::toResponse);

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to get matches", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to retrieve matches: " + e.getMessage()));
        }
    }

    @GetMapping("/cv/{cvId}/above-threshold")
    @Operation(summary = "Get matches above threshold", description = "Retrieve matches with score above minimum threshold")
    public ResponseEntity<ApiResponse<List<MatchResultResponse>>> getMatchesAboveThreshold(
        @PathVariable UUID cvId,
        @RequestParam UUID tenantId,
        @RequestParam BigDecimal minScore
    ) {
        try {
            securityUtils.validateTenantOwnership(tenantId);
            List<MatchResult> matches = matchingService.getMatchResultsAboveThreshold(cvId, tenantId, minScore);
            List<MatchResultResponse> response = matches.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to get matches", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to retrieve matches: " + e.getMessage()));
        }
    }

    @PostMapping("/cv/{cvId}/recompute")
    @Operation(summary = "Recompute matches", description = "Recompute all matches for a CV (clears existing results)")
    public ResponseEntity<ApiResponse<String>> recomputeMatches(
        @PathVariable UUID cvId,
        @RequestParam UUID tenantId
    ) {
        try {
            securityUtils.validateTenantOwnership(tenantId);
            matchingService.recomputeMatches(cvId, tenantId);
            return ResponseEntity.ok(ApiResponse.success("Match recomputation started"));
        } catch (Exception e) {
            log.error("Failed to recompute matches", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to recompute matches: " + e.getMessage()));
        }
    }

    private MatchResultResponse toResponse(MatchResult match) {
        List<EmailOptionResponse> options = null;
        if (match.getEmailLog() != null && match.getEmailLog().getOptions() != null) {
            options = match.getEmailLog().getOptions().stream()
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
}
