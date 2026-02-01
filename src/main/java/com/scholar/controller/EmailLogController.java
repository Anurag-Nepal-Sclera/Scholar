package com.scholar.controller;

import com.scholar.config.security.SecurityUtils;
import com.scholar.domain.entity.EmailLog;
import com.scholar.dto.response.ApiResponse;
import com.scholar.dto.response.EmailLogResponse;
import com.scholar.service.email.EmailCampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for managing individual email logs (drafts).
 */
@RestController
@RequestMapping("/v1/logs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Email Logs", description = "APIs for managing individual email drafts and logs")
public class EmailLogController {

    private final EmailCampaignService campaignService;
    private final SecurityUtils securityUtils;

    @GetMapping("/{logId}")
    @Operation(summary = "Get email log details", description = "Retrieve full details of a specific email log including body and alternates")
    public ResponseEntity<ApiResponse<EmailLogResponse>> getEmailLog(
        @PathVariable UUID logId,
        @RequestParam UUID tenantId
    ) {
        try {
            securityUtils.validateTenantOwnership(tenantId);
            EmailLog logEntry = campaignService.getEmailLog(logId);
            
            // Validate tenant access
            if (!logEntry.getTenant().getId().equals(tenantId)) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }

            return ResponseEntity.ok(ApiResponse.success(toLogResponse(logEntry)));
        } catch (Exception e) {
            log.error("Failed to get email log", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve email log: " + e.getMessage()));
        }
    }

    @PutMapping("/{logId}")
    @Operation(summary = "Update email draft", description = "Manually update the email body for a specific log")
    public ResponseEntity<ApiResponse<EmailLogResponse>> updateEmailDraft(
        @PathVariable UUID logId,
        @RequestParam UUID tenantId,
        @Valid @RequestBody UpdateEmailDraftRequest request
    ) {
        try {
            securityUtils.validateTenantOwnership(tenantId);
            EmailLog updatedLog = campaignService.updateEmailLogBody(logId, request.getBody(), tenantId);
            return ResponseEntity.ok(ApiResponse.success(toLogResponse(updatedLog)));
        } catch (Exception e) {
            log.error("Failed to update email draft", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update draft: " + e.getMessage()));
        }
    }

    @PostMapping("/{logId}/regenerate")
    @Operation(summary = "Regenerate email draft", description = "Trigger AI to regenerate the email draft for this specific log")
    public ResponseEntity<ApiResponse<EmailLogResponse>> regenerateEmailDraft(
        @PathVariable UUID logId,
        @RequestParam UUID tenantId
    ) {
        try {
            securityUtils.validateTenantOwnership(tenantId);
            EmailLog regeneratedLog = campaignService.regenerateEmailLog(logId, tenantId);
            return ResponseEntity.ok(ApiResponse.success(toLogResponse(regeneratedLog)));
        } catch (Exception e) {
            log.error("Failed to regenerate email draft", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to regenerate draft: " + e.getMessage()));
        }
    }

    @PostMapping("/{logId}/send")
    @Operation(summary = "Send individual email", description = "Immediately send this specific email")
    public ResponseEntity<ApiResponse<String>> sendIndividualEmail(
        @PathVariable UUID logId,
        @RequestParam UUID tenantId
    ) {
        try {
            securityUtils.validateTenantOwnership(tenantId);
            campaignService.sendIndividualEmail(logId, tenantId);
            return ResponseEntity.ok(ApiResponse.success("Email sent successfully"));
        } catch (Exception e) {
            log.error("Failed to send individual email", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to send email: " + e.getMessage()));
        }
    }

    private EmailLogResponse toLogResponse(EmailLog log) {
        return EmailLogResponse.builder()
            .id(log.getId())
            .recipientEmail(log.getRecipientEmail())
            .subject(log.getSubject())
            .body(log.getBody()) // Include full body
            .alternateBodies(log.getAlternateBodies()) // Include alternates
            .status(log.getStatus().name())
            .errorMessage(log.getErrorMessage())
            .retryCount(log.getRetryCount())
            .sentAt(log.getSentAt())
            .createdAt(log.getCreatedAt())
            .professorId(log.getProfessor().getId())
            .professorName(log.getProfessor().getFirstName() + " " + log.getProfessor().getLastName())
            .build();
    }

    @Data
    public static class UpdateEmailDraftRequest {
        @NotNull
        private String body;
    }
}
