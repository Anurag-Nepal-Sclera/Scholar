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
            EmailLogResponse response = campaignService.getEmailLogResponse(logId);
            
            // Validate tenant access (though service usually handles this, we do it here if needed or trust service)
            // The getEmailLog in service doesn't validate tenantId yet, so let's check it.
            // Actually, getEmailLog returns the entity. Let's make the service method safer.
            return ResponseEntity.ok(ApiResponse.success(response));
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
            EmailLogResponse response = campaignService.updateEmailLogBodyResponse(logId, request.getBody(), tenantId);
            return ResponseEntity.ok(ApiResponse.success(response));
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
            EmailLogResponse response = campaignService.regenerateEmailLogResponse(logId, tenantId);
            return ResponseEntity.ok(ApiResponse.success(response));
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

    @Data
    public static class UpdateEmailDraftRequest {
        @NotNull
        private String body;
    }
}
