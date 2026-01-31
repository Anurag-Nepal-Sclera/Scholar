package com.scholar.service.email;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Event published when an SMTP account is created, updated, or deactivated.
 */
@Getter
public class SmtpAccountChangedEvent extends ApplicationEvent {
    private final UUID accountId;
    private final UUID tenantId;

    public SmtpAccountChangedEvent(Object source, UUID accountId, UUID tenantId) {
        super(source);
        this.accountId = accountId;
        this.tenantId = tenantId;
    }
}
