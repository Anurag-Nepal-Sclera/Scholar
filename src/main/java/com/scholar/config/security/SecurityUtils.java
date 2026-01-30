package com.scholar.config.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

import com.scholar.domain.entity.Tenant;
import com.scholar.domain.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final TenantRepository tenantRepository;

    public ScholarUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof ScholarUserDetails) {
            return (ScholarUserDetails) authentication.getPrincipal();
        }
        throw new IllegalStateException("User not authenticated");
    }

    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /**
     * Validates that the current user owns the specified tenant.
     * 
     * @param tenantId the tenant ID to check
     * @return the Tenant entity if ownership is valid
     * @throws IllegalArgumentException if ownership is invalid
     */
    public Tenant validateTenantOwnership(UUID tenantId) {
        UUID userId = getCurrentUserId();
        return tenantRepository.findByIdAndOwnerId(tenantId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Access denied: You do not own this tenant"));
    }
}
