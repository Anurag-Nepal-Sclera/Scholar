package com.scholar.service;

import com.scholar.domain.entity.Tenant;
import com.scholar.domain.entity.UserProfile;
import com.scholar.domain.repository.TenantRepository;
import com.scholar.domain.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserProfileRepository userRepository;

    @Transactional
    public Tenant createTenant(UUID ownerId, String name, String email) {
        UserProfile owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Tenant tenant = Tenant.builder()
                .owner(owner)
                .name(name)
                .email(email)
                .status(Tenant.TenantStatus.ACTIVE)
                .build();

        return tenantRepository.save(tenant);
    }

    @Transactional(readOnly = true)
    public List<Tenant> getTenantsByOwner(UUID ownerId) {
        // We need a method in repository for this
        return tenantRepository.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public Tenant getTenantByIdAndOwner(UUID tenantId, UUID ownerId) {
        return tenantRepository.findByIdAndOwnerId(tenantId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found or access denied"));
    }

    @Transactional
    public void deleteTenant(UUID tenantId, UUID ownerId) {
        Tenant tenant = getTenantByIdAndOwner(tenantId, ownerId);
        tenantRepository.delete(tenant);
    }
}
