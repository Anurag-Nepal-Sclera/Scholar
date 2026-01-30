package com.scholar.controller;

import com.scholar.config.security.SecurityUtils;
import com.scholar.domain.entity.Tenant;
import com.scholar.dto.request.TenantRequest;
import com.scholar.dto.response.ApiResponse;
import com.scholar.dto.response.TenantResponse;
import com.scholar.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenant Management", description = "Endpoints for managing user organizations")
public class TenantController {

    private final TenantService tenantService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @Operation(summary = "Create a new tenant", description = "Create a new organization for the current user")
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(
            @Valid @RequestBody TenantRequest request
    ) {
        UUID userId = securityUtils.getCurrentUserId();
        Tenant tenant = tenantService.createTenant(userId, request.getName(), request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Tenant created successfully", mapToResponse(tenant)));
    }

    @GetMapping
    @Operation(summary = "List my tenants", description = "Get all organizations owned by the current user")
    public ResponseEntity<ApiResponse<List<TenantResponse>>> listMyTenants() {
        UUID userId = securityUtils.getCurrentUserId();
        List<Tenant> tenants = tenantService.getTenantsByOwner(userId);
        List<TenantResponse> response = tenants.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{tenantId}")
    @Operation(summary = "Delete tenant", description = "Delete an organization and all its data")
    public ResponseEntity<ApiResponse<String>> deleteTenant(@PathVariable UUID tenantId) {
        UUID userId = securityUtils.getCurrentUserId();
        tenantService.deleteTenant(tenantId, userId);
        return ResponseEntity.ok(ApiResponse.success("Tenant deleted successfully"));
    }

    private TenantResponse mapToResponse(Tenant tenant) {
        return TenantResponse.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .email(tenant.getEmail())
                .status(tenant.getStatus().name())
                .build();
    }
}
