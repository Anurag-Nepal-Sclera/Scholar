package com.scholar.service.email;

import com.scholar.domain.entity.SmtpAccount;
import com.scholar.dto.response.IncomingEmailResponse;
import com.scholar.service.security.EncryptionService;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncomingEmailService {

    private final EncryptionService encryptionService;

    public List<IncomingEmailResponse> fetchIncomingEmails(SmtpAccount smtpAccount) {
        List<IncomingEmailResponse> emails = new ArrayList<>();
        Properties props = new Properties();
        
        // Use IMAP for incoming emails (usually imap.gmail.com)
        // Derive IMAP host from SMTP host if common providers
        String imapHost = smtpAccount.getSmtpHost().replace("smtp", "imap");
        
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", imapHost);
        props.put("mail.imaps.port", "993");
        props.put("mail.imaps.ssl.enable", "true");

        try {
            Session session = Session.getInstance(props);
            Store store = session.getStore("imaps");
            
            String password = encryptionService.decrypt(smtpAccount.getEncryptedPassword());
            store.connect(imapHost, smtpAccount.getUsername(), password);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            // Fetch last 20 messages
            int count = inbox.getMessageCount();
            int start = Math.max(1, count - 19);
            Message[] messages = inbox.getMessages(start, count);

            for (int i = messages.length - 1; i >= 0; i--) {
                Message msg = messages[i];
                emails.add(IncomingEmailResponse.builder()
                        .from(msg.getFrom()[0].toString())
                        .subject(msg.getSubject())
                        .receivedDate(msg.getReceivedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                        .bodyPreview(getTextFromMessage(msg).substring(0, Math.min(200, getTextFromMessage(msg).length())))
                        .build());
            }

            inbox.close(false);
            store.close();
        } catch (Exception e) {
            log.error("Failed to fetch incoming emails for tenant: {}", smtpAccount.getTenant().getId(), e);
            throw new RuntimeException("Could not fetch emails. Please verify IMAP settings on your provider.", e);
        }

        return emails;
    }

    private String getTextFromMessage(Message message) throws Exception {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) message.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    return bp.getContent().toString();
                }
            }
        }
        return "No text content available";
    }
}
