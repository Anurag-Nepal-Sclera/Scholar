package com.scholar.service.email;

import com.scholar.domain.entity.SmtpAccount;
import com.scholar.domain.entity.Tenant;
import com.scholar.domain.repository.SmtpAccountRepository;
import com.scholar.domain.repository.TenantRepository;
import com.scholar.service.security.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing SMTP accounts with encrypted credentials.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpAccountService {

    private final SmtpAccountRepository smtpAccountRepository;
    private final TenantRepository tenantRepository;
    private final EncryptionService encryptionService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Creates or updates an SMTP account for a tenant.
     * 
     * @param tenantId the tenant identifier
     * @param email the email address
     * @param smtpHost the SMTP host
     * @param smtpPort the SMTP port
     * @param username the SMTP username
     * @param password the SMTP password (will be encrypted)
     * @param useTls whether to use TLS
     * @param useSsl whether to use SSL
     * @param fromName the from name for emails
     * @return the created/updated SMTP account
     */
    @Transactional
    public SmtpAccount saveSmtpAccount(
        UUID tenantId,
        String email,
        String smtpHost,
        Integer smtpPort,
        String username,
        String password,
        Boolean useTls,
        Boolean useSsl,
        String fromName
    ) {
        // Encrypt password
        String encryptedPassword = encryptionService.encrypt(password);

        SmtpAccount smtpAccount = smtpAccountRepository.findByTenantId(tenantId)
            .orElseGet(() -> {
                Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
                return SmtpAccount.builder()
                    .tenant(tenant)
                    .build();
            });

        smtpAccount.setEmail(email);
        smtpAccount.setSmtpHost(smtpHost);
        smtpAccount.setSmtpPort(smtpPort);
        smtpAccount.setUsername(username);
        smtpAccount.setEncryptedPassword(encryptedPassword);
        smtpAccount.setUseTls(useTls != null ? useTls : true);
        smtpAccount.setUseSsl(useSsl != null ? useSsl : false);
        smtpAccount.setFromName(fromName);
        smtpAccount.setStatus(SmtpAccount.SmtpAccountStatus.ACTIVE);

        SmtpAccount saved = smtpAccountRepository.save(smtpAccount);
        log.info("SMTP account saved for tenant: {}", tenantId);
        
        eventPublisher.publishEvent(new SmtpAccountChangedEvent(this, saved.getId(), tenantId));
        
        return saved;
    }

    /**
     * Retrieves SMTP account for a tenant.
     * 
     * @param tenantId the tenant identifier
     * @return the SMTP account
     */
    @Transactional(readOnly = true)
    public SmtpAccount getSmtpAccount(UUID tenantId) {
        return smtpAccountRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("SMTP account not found for tenant: " + tenantId));
    }

    /**
     * Retrieves active SMTP account for a tenant.
     * 
     * @param tenantId the tenant identifier
     * @return the active SMTP account
     */
    @Transactional(readOnly = true)
    public SmtpAccount getActiveSmtpAccount(UUID tenantId) {
        return smtpAccountRepository.findActiveByTenantId(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Active SMTP account not found for tenant: " + tenantId));
    }

    /**
     * Decrypts the SMTP account password.
     * 
     * @param smtpAccount the SMTP account
     * @return decrypted password
     */
    public String decryptPassword(SmtpAccount smtpAccount) {
        return encryptionService.decrypt(smtpAccount.getEncryptedPassword());
    }

    /**
     * Deactivates an SMTP account.
     * 
     * @param tenantId the tenant identifier
     */
    @Transactional
    public void deactivateSmtpAccount(UUID tenantId) {
        SmtpAccount smtpAccount = getSmtpAccount(tenantId);
        smtpAccount.setStatus(SmtpAccount.SmtpAccountStatus.INACTIVE);
        smtpAccountRepository.save(smtpAccount);
        log.info("SMTP account deactivated for tenant: {}", tenantId);
        
        eventPublisher.publishEvent(new SmtpAccountChangedEvent(this, smtpAccount.getId(), tenantId));
    }
}
