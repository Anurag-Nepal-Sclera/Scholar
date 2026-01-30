package com.scholar.controller;

import com.scholar.config.security.SecurityUtils;
import com.scholar.domain.entity.EmailCampaign;
import com.scholar.domain.entity.EmailLog;
import com.scholar.dto.request.CreateCampaignRequest;
import com.scholar.dto.response.ApiResponse;
import com.scholar.dto.response.EmailCampaignResponse;
import com.scholar.dto.response.EmailLogResponse;
import com.scholar.service.email.EmailCampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REST controller for email campaign operations.
 */
@RestController
@RequestMapping("/v1/campaigns")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Email Campaigns", description = "APIs for managing email outreach campaigns")
public class EmailCampaignController {

    private final EmailCampaignService campaignService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @Operation(summary = "Create campaign", description = "Create a new email campaign")
    public ResponseEntity<ApiResponse<EmailCampaignResponse>> createCampaign(
        @RequestParam UUID tenantId,
        @Valid @RequestBody CreateCampaignRequest request
    ) {
        try {
            securityUtils.validateTenantOwnership(tenantId);
            EmailCampaign campaign = campaignService.createCampaign(
                tenantId,
                request.getCvId(),
                request.getName(),
                request.getSubject(),
                request.getBodyTemplate(),
                request.getMinMatchScore()
            );

            EmailCampaignResponse response = toResponse(campaign);
            return ResponseEntity.ok(ApiResponse.success("Campaign created successfully", response));
        } catch (Exception e) {
            log.error("Failed to create campaign", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to create campaign: " + e.getMessage()));
        }
    }

    @PostMapping("/{campaignId}/schedule")
    @Operation(summary = "Schedule campaign", description = "Schedule a campaign for execution")
    public ResponseEntity<ApiResponse<String>> scheduleCampaign(
        @PathVariable UUID campaignId,
        @RequestParam UUID tenantId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledAt
    ) {
        try {
            securityUtils.validateTenantOwnership(tenantId);
            campaignService.scheduleCampaign(campaignId, tenantId, scheduledAt);
            return ResponseEntity.ok(ApiResponse.success("Campaign scheduled successfully"));
        } catch (Exception e) {
            log.error("Failed to schedule campaign", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to schedule campaign: " + e.getMessage()));
        }
    }

    @PostMapping("/{campaignId}/execute")
    @Operation(summary = "Execute campaign", description = "Execute a campaign immediately (async)")
    public ResponseEntity<ApiResponse<String>> executeCampaign(
        @PathVariable UUID campaignId,
        @RequestParam UUID tenantId
    ) {
        try {
            securityUtils.validateTenantOwnership(tenantId);
            // Just trigger - the service now handles the logic of finding matches/recipients
            campaignService.executeCampaign(campaignId);
            return ResponseEntity.ok(ApiResponse.success("Campaign execution started"));
        } catch (Exception e) {
            log.error("Failed to execute campaign", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to execute campaign: " + e.getMessage()));
        }
    }

    @PostMapping("/{campaignId}/cancel")
    @Operation(summary = "Cancel campaign", description = "Cancel a scheduled campaign")
    public ResponseEntity<ApiResponse<String>> cancelCampaign(
        @PathVariable UUID campaignId,
        @RequestParam UUID tenantId
    ) {
        try {
            securityUtils.validateTenantOwnership(tenantId);
            campaignService.cancelCampaign(campaignId, tenantId);
            return ResponseEntity.ok(ApiResponse.success("Campaign cancelled successfully"));
        } catch (Exception e) {
            log.error("Failed to cancel campaign", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to cancel campaign: " + e.getMessage()));
        }
    }

    @GetMapping("/{campaignId}")
    @Operation(summary = "Get campaign details", description = "Retrieve details of a specific campaign")
    public ResponseEntity<ApiResponse<EmailCampaignResponse>> getCampaign(
        @PathVariable UUID campaignId,
        @RequestParam UUID tenantId
    ) {
        try {
            securityUtils.validateTenantOwnership(tenantId);
            EmailCampaign campaign = campaignService.getCampaign(campaignId, tenantId);
            EmailCampaignResponse response = toResponse(campaign);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to get campaign", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to retrieve campaign: " + e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "List all campaigns", description = "Retrieve all campaigns for a tenant with pagination")
    public ResponseEntity<ApiResponse<Page<EmailCampaignResponse>>> getAllCampaigns(
        @RequestParam UUID tenantId,
        @Parameter(hidden = true) Pageable pageable
    ) {
        try {
            securityUtils.validateTenantOwnership(tenantId);
            Page<EmailCampaign> campaigns = campaignService.getAllCampaigns(tenantId, pageable);
            Page<EmailCampaignResponse> response = campaigns.map(this::toResponse);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to get campaigns", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to retrieve campaigns: " + e.getMessage()));
        }
    }

    @GetMapping("/{campaignId}/logs")
    @Operation(summary = "Get campaign logs", description = "Retrieve email logs for a campaign")
    public ResponseEntity<ApiResponse<Page<EmailLogResponse>>> getCampaignLogs(
        @PathVariable UUID campaignId,
        @RequestParam UUID tenantId,
        @Parameter(hidden = true) Pageable pageable
    ) {
        try {
            securityUtils.validateTenantOwnership(tenantId);
            Page<EmailLog> logs = campaignService.getCampaignLogs(campaignId, pageable);
            Page<EmailLogResponse> response = logs.map(this::toLogResponse);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to get campaign logs", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to retrieve campaign logs: " + e.getMessage()));
        }
    }

    private EmailCampaignResponse toResponse(EmailCampaign campaign) {
        return EmailCampaignResponse.builder()
            .id(campaign.getId())
            .name(campaign.getName())
            .subject(campaign.getSubject())
            .minMatchScore(campaign.getMinMatchScore())
            .status(campaign.getStatus().name())
            .totalRecipients(campaign.getTotalRecipients())
            .sentCount(campaign.getSentCount())
            .failedCount(campaign.getFailedCount())
            .scheduledAt(campaign.getScheduledAt())
            .startedAt(campaign.getStartedAt())
            .completedAt(campaign.getCompletedAt())
            .createdAt(campaign.getCreatedAt())
            .build();
    }

    private EmailLogResponse toLogResponse(EmailLog log) {
        return EmailLogResponse.builder()
            .id(log.getId())
            .recipientEmail(log.getRecipientEmail())
            .subject(log.getSubject())
            .status(log.getStatus().name())
            .errorMessage(log.getErrorMessage())
            .retryCount(log.getRetryCount())
            .sentAt(log.getSentAt())
            .createdAt(log.getCreatedAt())
            .professorId(log.getProfessor().getId())
            .build();
    }
}
