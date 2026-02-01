package com.scholar.service.email;

import com.scholar.domain.entity.SmtpAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory and cache for JavaMailSender instances based on SMTP accounts.
 * Ensures efficient reuse of mail sender objects.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MailSenderFactory {

    private final SmtpAccountService smtpAccountService;
    private final Map<UUID, JavaMailSender> senderCache = new ConcurrentHashMap<>();

    /**
     * Gets a JavaMailSender for the given SMTP account, creating it if not cached.
     *
     * @param smtpAccount the SMTP account configuration
     * @return the JavaMailSender
     */
    public JavaMailSender getMailSender(SmtpAccount smtpAccount) {
        return senderCache.compute(smtpAccount.getId(), (id, existing) -> {
            if (existing != null) {
                return existing;
            }
            return createMailSender(smtpAccount);
        });
    }

    /**
     * Listens for SMTP account changes and clears the relevant cache entry.
     *
     * @param event the SMTP account changed event
     */
    @EventListener
    public void handleSmtpAccountChanged(SmtpAccountChangedEvent event) {
        log.info("Received SmtpAccountChangedEvent for account: {}. Clearing cache.", event.getAccountId());
        clearCache(event.getAccountId());
    }

    /**
     * Clears the cached sender for a specific account.
     *
     * @param accountId the SMTP account identifier
     */
    public void clearCache(UUID accountId) {
        senderCache.remove(accountId);
        log.debug("Cleared MailSender cache for account: {}", accountId);
    }

    private JavaMailSender createMailSender(SmtpAccount s) {
        log.info("Creating new JavaMailSender for account: {}", s.getEmail());
        JavaMailSenderImpl ms = new JavaMailSenderImpl();
        ms.setHost(s.getSmtpHost());
        ms.setPort(s.getSmtpPort());
        ms.setUsername(s.getUsername());
        ms.setPassword(smtpAccountService.decryptPassword(s));

        Properties props = ms.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", 5000);
        props.put("mail.smtp.timeout", 5000);
        props.put("mail.smtp.writetimeout", 5000);

        if (Boolean.TRUE.equals(s.getUseTls())) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        if (Boolean.TRUE.equals(s.getUseSsl())) {
            props.put("mail.smtp.ssl.enable", "true");
        }
        
        return ms;
    }
}
