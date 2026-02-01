package com.scholar.service.email;

import com.scholar.domain.entity.SmtpAccount;
import com.scholar.domain.repository.SmtpAccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("prod")
@Slf4j
public class SmtpDiagnosticTest {

    @Autowired
    private SmtpAccountRepository smtpAccountRepository;

    @Autowired
    private SmtpAccountService smtpAccountService;

    @Test
    public void testSmtpConfiguration() {
        log.info("Starting SMTP diagnostic test...");
        
        List<SmtpAccount> accounts = smtpAccountRepository.findAll();
        assertFalse(accounts.isEmpty(), "No SMTP accounts found in the database. Please add one first.");
        
        SmtpAccount account = accounts.stream()
                .filter(a -> a.getStatus() == SmtpAccount.SmtpAccountStatus.ACTIVE)
                .findFirst()
                .orElse(accounts.get(0));
        
        log.info("Testing SMTP account: {} ({})", account.getEmail(), account.getSmtpHost());
        log.info("Status: {}", account.getStatus());
        
        try {
            JavaMailSender mailSender = createMailSender(account);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(account.getEmail());
            message.setTo(account.getEmail()); // Send to self for testing
            message.setSubject("Scholar SMTP Diagnostic Test");
            message.setText("This is a test email sent from the Scholar application to verify SMTP configuration.\n\n" +
                    "SMTP Host: " + account.getSmtpHost() + "\n" +
                    "SMTP Port: " + account.getSmtpPort() + "\n" +
                    "Username: " + account.getUsername());
            
            log.info("Sending test email to: {}", account.getEmail());
            mailSender.send(message);
            log.info("SMTP test email sent successfully!");
            
        } catch (Exception e) {
            log.error("SMTP diagnostic test failed", e);
            fail("Failed to send test email: " + e.getMessage());
        }
    }

    private JavaMailSender createMailSender(SmtpAccount s) {
        JavaMailSenderImpl ms = new JavaMailSenderImpl();
        ms.setHost(s.getSmtpHost()); 
        ms.setPort(s.getSmtpPort());
        ms.setUsername(s.getUsername()); 
        ms.setPassword(smtpAccountService.decryptPassword(s));
        
        Properties p = ms.getJavaMailProperties();
        p.put("mail.smtp.auth", "true");
        p.put("mail.smtp.timeout", "5000");
        p.put("mail.smtp.connectiontimeout", "5000");
        p.put("mail.smtp.writetimeout", "5000");
        
        if (s.getUseTls()) { 
            p.put("mail.smtp.starttls.enable", "true"); 
            p.put("mail.smtp.starttls.required", "true"); 
        }
        if (s.getUseSsl()) {
            p.put("mail.smtp.ssl.enable", "true");
        }
        
        // Help with debug
        p.put("mail.debug", "true");
        
        return ms;
    }
}
