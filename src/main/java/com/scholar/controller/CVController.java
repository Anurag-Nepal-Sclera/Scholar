package com.scholar.controller;

import com.scholar.config.security.SecurityUtils;
import com.scholar.domain.entity.CV;
import com.scholar.domain.entity.Tenant;
import com.scholar.domain.entity.UserProfile;
import com.scholar.domain.repository.UserProfileRepository;
import com.scholar.dto.response.ApiResponse;
import com.scholar.dto.response.CVResponse;
import com.scholar.service.cv.CVService;
import com.scholar.service.matching.MatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST controller for CV management operations.
 */
@RestController
@RequestMapping("/v1/cvs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "CV Management", description = "APIs for uploading and managing CVs")
public class CVController {

    private final CVService cvService;
    private final MatchingService matchingService;
    private final SecurityUtils securityUtils;
    private final UserProfileRepository userProfileRepository;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a CV", description = "Upload a PDF or DOCX CV file for parsing and keyword extraction")
    public ResponseEntity<ApiResponse<CVResponse>> uploadCV(
        @RequestParam("file") MultipartFile file,
        @RequestParam("tenantId") UUID tenantId
    ) {
        try {
            Tenant tenant = securityUtils.validateTenantOwnership(tenantId);
            UUID userProfileId = securityUtils.getCurrentUserId();
            
            UserProfile userProfile = userProfileRepository.findById(userProfileId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            CV cv = cvService.uploadCV(file, tenant, userProfile);
            
            CVResponse response = CVResponse.builder()
                .id(cv.getId())
                .originalFilename(cv.getOriginalFilename())
                .fileSizeBytes(cv.getFileSizeBytes())
                .mimeType(cv.getMimeType())
                .parsingStatus(cv.getParsingStatus().name())
                .uploadedAt(cv.getUploadedAt())
                .build();

            return ResponseEntity.ok(ApiResponse.success("CV uploaded successfully", response));
        } catch (Exception e) {
            log.error("Failed to upload CV", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("CV upload failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{cvId}")
    @Operation(summary = "Get CV details", description = "Retrieve details of a specific CV")
    public ResponseEntity<ApiResponse<CVResponse>> getCV(
        @PathVariable UUID cvId,
        @RequestParam UUID tenantId
    ) {
        try {
            securityUtils.validateTenantOwnership(tenantId);
            CV cv = cvService.getCV(cvId, tenantId);
            int keywordCount = cvService.getKeywordCount(cvId);
            
            CVResponse response = CVResponse.builder()
                .id(cv.getId())
                .originalFilename(cv.getOriginalFilename())
                .fileSizeBytes(cv.getFileSizeBytes())
                .mimeType(cv.getMimeType())
                .parsingStatus(cv.getParsingStatus().name())
                .parsedAt(cv.getParsedAt())
                .uploadedAt(cv.getUploadedAt())
                .keywordCount(keywordCount)
                .build();

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to get CV", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to retrieve CV: " + e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "List all CVs", description = "Retrieve all CVs for a tenant with pagination")
    public ResponseEntity<ApiResponse<Page<CVResponse>>> getAllCVs(
        @RequestParam UUID tenantId,
        @Parameter(hidden = true) Pageable pageable
    ) {
        try {
            securityUtils.validateTenantOwnership(tenantId);
            Page<CV> cvs = cvService.getAllCVs(tenantId, pageable);
            Page<CVResponse> response = cvs.map(cv -> CVResponse.builder()
                .id(cv.getId())
                .originalFilename(cv.getOriginalFilename())
                .fileSizeBytes(cv.getFileSizeBytes())
                .mimeType(cv.getMimeType())
                .parsingStatus(cv.getParsingStatus().name())
                .parsedAt(cv.getParsedAt())
                .uploadedAt(cv.getUploadedAt())
                .build());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to get CVs", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to retrieve CVs: " + e.getMessage()));
        }
    }

    @PostMapping("/{cvId}/parse")
    @Operation(summary = "Parse CV", description = "Manually trigger CV parsing and keyword extraction")
    public ResponseEntity<ApiResponse<String>> parseCV(
        @PathVariable UUID cvId,
        @RequestParam UUID tenantId
    ) {
        try {
            securityUtils.validateTenantOwnership(tenantId);
            CV cv = cvService.getCV(cvId, tenantId);
            
            if (cv.getParsingStatus() == CV.ParsingStatus.IN_PROGRESS) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Parsing is already in progress"));
            }
            
            cvService.parseAndExtractKeywords(cvId);
            return ResponseEntity.ok(ApiResponse.success("CV parsing started"));
        } catch (Exception e) {
            log.error("Failed to start CV parsing", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to start parsing: " + e.getMessage()));
        }
    }

    @PostMapping("/{cvId}/compute-matches")
    @Operation(summary = "Compute matches", description = "Compute professor matches for a CV based on keywords")
    public ResponseEntity<ApiResponse<String>> computeMatches(
        @PathVariable UUID cvId,
        @RequestParam UUID tenantId
    ) {
        try {
            securityUtils.validateTenantOwnership(tenantId);
            CV cv = cvService.getCV(cvId, tenantId);
            
            if (cv.getParsingStatus() != CV.ParsingStatus.COMPLETED) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("CV must be in COMPLETED parsing status before computing matches. Current status: " + cv.getParsingStatus()));
            }
            
            matchingService.computeMatches(cvId, tenantId);
            return ResponseEntity.ok(ApiResponse.success("Match computation started"));
        } catch (Exception e) {
            log.error("Failed to compute matches", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to compute matches: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{cvId}")
    @Operation(summary = "Delete CV", description = "Delete a CV and all associated data")
    public ResponseEntity<ApiResponse<String>> deleteCV(
        @PathVariable UUID cvId,
        @RequestParam UUID tenantId
    ) {
        try {
            securityUtils.validateTenantOwnership(tenantId);
            cvService.deleteCV(cvId, tenantId);
            return ResponseEntity.ok(ApiResponse.success("CV deleted successfully"));
        } catch (Exception e) {
            log.error("Failed to delete CV", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to delete CV: " + e.getMessage()));
        }
    }
}
