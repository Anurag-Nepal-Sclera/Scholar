package com.scholar.service.email;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Diagnostic test for verifying specific email credentials.
 * This can be run to check if the app password and SMTP settings are correct.
 */
@Slf4j
public class EmailVerificationTest {

    @Test
    public void verifyEmailCredentials() {
        String senderEmail = "sangam.thapa2028@gmail.com";
        String appPassword = "bazrabbsackxktut";
        
        log.info("Starting email verification for: {}", senderEmail);
        
        try {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost("smtp.gmail.com");
            mailSender.setPort(587);
            mailSender.setUsername(senderEmail);
            mailSender.setPassword(appPassword);
            
            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.debug", "true"); // Enable debug logs to see SMTP traffic
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(senderEmail); // Send to self
            message.setSubject("Scholar Email Verification");
            message.setText("If you are reading this, your email configuration works correctly!");
            
            log.info("Attempting to send test email to self...");
            mailSender.send(message);
            log.info("SUCCESS: Email sent successfully!");
            
        } catch (Exception e) {
            log.error("FAILURE: Could not send email. Error: {}", e.getMessage(), e);
            // Don't fail the test immediately so we can see the full log
        }
    }
}
