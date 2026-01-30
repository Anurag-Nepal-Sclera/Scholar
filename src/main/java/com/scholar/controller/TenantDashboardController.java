package com.scholar.controller;

import com.scholar.config.security.SecurityUtils;
import com.scholar.domain.entity.SmtpAccount;
import com.scholar.dto.response.ApiResponse;
import com.scholar.dto.response.IncomingEmailResponse;
import com.scholar.dto.response.TenantDashboardResponse;
import com.scholar.service.TenantDashboardService;
import com.scholar.service.email.IncomingEmailService;
import com.scholar.service.email.SmtpAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/tenants/{tenantId}")
@RequiredArgsConstructor
@Tag(name = "Tenant Dashboard", description = "Endpoints for tenant tracking and analytics")
public class TenantDashboardController {

    private final TenantDashboardService dashboardService;
    private final IncomingEmailService incomingEmailService;
    private final SmtpAccountService smtpAccountService;
    private final SecurityUtils securityUtils;

    @GetMapping("/stats")
    @Operation(summary = "Get tenant stats", description = "Retrieve summary statistics for a tenant")
    public ResponseEntity<ApiResponse<TenantDashboardResponse>> getStats(@PathVariable UUID tenantId) {
        securityUtils.validateTenantOwnership(tenantId);
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboardStats(tenantId)));
    }

    @GetMapping("/incoming-emails")
    @Operation(summary = "Get incoming emails", description = "Fetch recent incoming emails from the inbox associated with the tenant")
    public ResponseEntity<ApiResponse<List<IncomingEmailResponse>>> getIncomingEmails(@PathVariable UUID tenantId) {
        securityUtils.validateTenantOwnership(tenantId);
        SmtpAccount smtpAccount = smtpAccountService.getActiveSmtpAccount(tenantId);
        return ResponseEntity.ok(ApiResponse.success(incomingEmailService.fetchIncomingEmails(smtpAccount)));
    }
}
