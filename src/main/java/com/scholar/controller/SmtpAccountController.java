package com.scholar.controller;

import com.scholar.config.security.SecurityUtils;
import com.scholar.domain.entity.SmtpAccount;
import com.scholar.dto.request.SmtpAccountRequest;
import com.scholar.dto.response.ApiResponse;
import com.scholar.dto.response.SmtpAccountResponse;
import com.scholar.service.email.SmtpAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for SMTP account management.
 */
@RestController
@RequestMapping("/v1/smtp")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SMTP Account", description = "APIs for managing SMTP email accounts")
public class SmtpAccountController {

    private final SmtpAccountService smtpAccountService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @Operation(summary = "Configure SMTP account", description = "Create or update SMTP account for a tenant")
    public ResponseEntity<ApiResponse<String>> configureSmtpAccount(
        @RequestParam UUID tenantId,
        @Valid @RequestBody SmtpAccountRequest request
    ) {
        try {
            securityUtils.validateTenantOwnership(tenantId);
            smtpAccountService.saveSmtpAccount(
                tenantId,
                request.getEmail(),
                request.getSmtpHost(),
                request.getSmtpPort(),
                request.getUsername(),
                request.getPassword(),
                request.getUseTls(),
                request.getUseSsl(),
                request.getFromName()
            );

            return ResponseEntity.ok(ApiResponse.success("SMTP account configured successfully"));
        } catch (Exception e) {
            log.error("Failed to configure SMTP account", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to configure SMTP account: " + e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "Get SMTP account", description = "Retrieve SMTP account for a tenant")
    public ResponseEntity<ApiResponse<SmtpAccountResponse>> getSmtpAccount(
        @RequestParam UUID tenantId
    ) {
        try {
            securityUtils.validateTenantOwnership(tenantId);
            SmtpAccount account = smtpAccountService.getSmtpAccount(tenantId);
            SmtpAccountResponse response = SmtpAccountResponse.builder()
                .id(account.getId())
                .email(account.getEmail())
                .smtpHost(account.getSmtpHost())
                .smtpPort(account.getSmtpPort())
                .username(account.getUsername())
                .useTls(account.getUseTls())
                .useSsl(account.getUseSsl())
                .fromName(account.getFromName())
                .status(account.getStatus().name())
                .build();
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to get SMTP account", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to retrieve SMTP account: " + e.getMessage()));
        }
    }

    @PostMapping("/deactivate")
    @Operation(summary = "Deactivate SMTP account", description = "Deactivate SMTP account for a tenant")
    public ResponseEntity<ApiResponse<String>> deactivateSmtpAccount(
        @RequestParam UUID tenantId
    ) {
        try {
            securityUtils.validateTenantOwnership(tenantId);
            smtpAccountService.deactivateSmtpAccount(tenantId);
            return ResponseEntity.ok(ApiResponse.success("SMTP account deactivated successfully"));
        } catch (Exception e) {
            log.error("Failed to deactivate SMTP account", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to deactivate SMTP account: " + e.getMessage()));
        }
    }
}
