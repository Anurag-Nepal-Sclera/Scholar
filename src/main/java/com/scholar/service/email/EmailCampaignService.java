package com.scholar.service.email;

import com.scholar.domain.entity.*;
import com.scholar.domain.repository.*;
import com.scholar.service.cv.OpenRouterService;
import com.scholar.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing email campaigns with batch processing and rate limiting.
 */
@Service
@Slf4j
public class EmailCampaignService {

    private final EmailCampaignRepository campaignRepository;
    private final EmailLogRepository emailLogRepository;
    private final EmailBlacklistRepository blacklistRepository;
    private final MatchResultRepository matchResultRepository;
    private final CvKeywordRepository cvKeywordRepository;
    private final CVRepository cvRepository;
    private final SmtpAccountService smtpAccountService;
    private final OpenRouterService openRouterService;
    private final FileStorageService fileStorageService;
    private final MailSenderFactory mailSenderFactory;
    private final EmailOptionRepository emailOptionRepository;
    private final EmailCampaignService self;

    public EmailCampaignService(EmailCampaignRepository campaignRepository,
                               EmailLogRepository emailLogRepository,
                               EmailBlacklistRepository blacklistRepository,
                               MatchResultRepository matchResultRepository,
                               CvKeywordRepository cvKeywordRepository,
                               CVRepository cvRepository,
                               SmtpAccountService smtpAccountService,
                               OpenRouterService openRouterService,
                               FileStorageService fileStorageService,
                               MailSenderFactory mailSenderFactory,
                               EmailOptionRepository emailOptionRepository,
                               @Lazy EmailCampaignService self) {
        this.campaignRepository = campaignRepository;
        this.emailLogRepository = emailLogRepository;
        this.blacklistRepository = blacklistRepository;
        this.matchResultRepository = matchResultRepository;
        this.cvKeywordRepository = cvKeywordRepository;
        this.cvRepository = cvRepository;
        this.smtpAccountService = smtpAccountService;
        this.openRouterService = openRouterService;
        this.fileStorageService = fileStorageService;
        this.mailSenderFactory = mailSenderFactory;
        this.emailOptionRepository = emailOptionRepository;
        this.self = self;
    }

    @Value("${scholar.email.batch-size}")
    private int batchSize;

    @Value("${scholar.email.rate-limit-per-minute}")
    private int rateLimitPerMinute;

    @Value("${scholar.email.retry-attempts:3}")
    private int retryAttempts;

    @Value("${scholar.email.retry-delay-ms:5000}")
    private long retryDelayMs;

    /**
     * Creates a new email campaign.
     */
    @Transactional
    public EmailCampaign createCampaign(UUID tenantId, UUID cvId, String name, String subject, String bodyTemplate, BigDecimal minMatchScore) {
        SmtpAccount smtpAccount = smtpAccountService.getActiveSmtpAccount(tenantId);
        CV cv = cvRepository.findById(cvId).orElseThrow();
        
        List<MatchResult> matches = matchResultRepository.findByCvIdAndTenantIdAndMinScore(cvId, tenantId, minMatchScore);

        EmailCampaign campaign = EmailCampaign.builder()
            .tenant(cv.getTenant())
            .cv(cv)
            .smtpAccount(smtpAccount)
            .name(name)
            .subject(subject)
            .bodyTemplate(bodyTemplate)
            .minMatchScore(minMatchScore)
            .totalRecipients(matches.size())
            .status(EmailCampaign.CampaignStatus.DRAFT)
            .build();

        return campaignRepository.save(campaign);
    }

    /**
     * Automatically creates a campaign based on high-relevance matches.
     */
    @Transactional
    public void createAutoCampaign(UUID cvId, UUID tenantId) {
        CV cv = cvRepository.findById(cvId).orElseThrow();
        BigDecimal autoThreshold = new BigDecimal("0.60");
        String campaignName = "AI-Outreach: " + cv.getOriginalFilename() + " (" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) + ")";
        
        EmailCampaign campaign = createCampaign(tenantId, cvId, campaignName, "Research Inquiry regarding interests matching your recent work", "AI_GENERATED", autoThreshold);

        // Initialize logs and trigger options generation
        List<MatchResult> matches = matchResultRepository.findByCvIdAndTenantIdAndMinScore(cvId, tenantId, autoThreshold);
        List<MatchResult> validMatches = matches.stream()
            .filter(match -> !blacklistRepository.isBlacklisted(match.getProfessor().getEmail(), tenantId))
            .collect(Collectors.toList());
            
        self.initializeEmailLogs(campaign.getId(), validMatches);
        
        // Trigger async options generation after transaction commit
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    self.prepareCampaignOptions(campaign.getId());
                }
            });
        } else {
            self.prepareCampaignOptions(campaign.getId());
        }
    }

    @Async
    public void executeCampaign(UUID campaignId) {
        try {
            if (!self.startCampaign(campaignId)) {
                log.warn("Campaign {} could not be started (already in progress or completed)", campaignId);
                return;
            }

            EmailCampaign campaign = campaignRepository.findById(campaignId).orElseThrow();
            byte[] cvFile = fileStorageService.retrieveFile(campaign.getCv().getFilePath());
            String fileName = campaign.getCv().getOriginalFilename();

            List<EmailLog> logs = emailLogRepository.findByEmailCampaignId(campaignId);
            List<UUID> emailLogIds = logs.stream().map(EmailLog::getId).collect(Collectors.toList());
            
            if (emailLogIds.isEmpty()) {
                // If logs weren't initialized (e.g. manually triggered non-auto campaign)
                List<MatchResult> matches = matchResultRepository.findByCvIdAndTenantIdAndMinScore(campaign.getCv().getId(), campaign.getTenant().getId(), campaign.getMinMatchScore());
                List<MatchResult> validMatches = matches.stream()
                    .filter(match -> !blacklistRepository.isBlacklisted(match.getProfessor().getEmail(), campaign.getTenant().getId()))
                    .collect(Collectors.toList());
                emailLogIds = self.initializeEmailLogs(campaignId, validMatches);
            }

            processBatchedEmails(campaignId, emailLogIds, cvFile, fileName);

            self.updateCampaignStatus(campaignId, EmailCampaign.CampaignStatus.COMPLETED, null, LocalDateTime.now());
        } catch (Exception e) {
            log.error("Campaign failed", e);
            self.updateCampaignStatus(campaignId, EmailCampaign.CampaignStatus.FAILED, null, null);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean startCampaign(UUID campaignId) {
        int updated = campaignRepository.updateStatusIfAllowed(
            campaignId,
            EmailCampaign.CampaignStatus.IN_PROGRESS,
            LocalDateTime.now(),
            Arrays.asList(EmailCampaign.CampaignStatus.SCHEDULED, EmailCampaign.CampaignStatus.DRAFT)
        );
        return updated > 0;
    }

    private void processBatchedEmails(UUID campaignId, List<UUID> emailLogIds, byte[] attachment, String fileName) {
        EmailCampaign campaign = campaignRepository.findById(campaignId).orElseThrow();
        SmtpAccount smtpAccount = campaign.getSmtpAccount();
        JavaMailSender mailSender = mailSenderFactory.getMailSender(smtpAccount);

        int processed = 0, sent = 0, failed = 0;
        long batchStartTime = System.currentTimeMillis();
        int emailsInCurrentMinute = 0;

        log.info("Starting batch processing for campaign {} with {} recipients", campaignId, emailLogIds.size());

        for (UUID emailLogId : emailLogIds) {
            try {
                // Rate limiting logic
                if (++emailsInCurrentMinute >= rateLimitPerMinute) {
                    long waitTime = 60000 - (System.currentTimeMillis() - batchStartTime);
                    if (waitTime > 0) {
                        log.info("Rate limit reached for campaign {}, waiting for {}ms", campaignId, waitTime);
                        Thread.sleep(waitTime);
                    }
                    batchStartTime = System.currentTimeMillis();
                    emailsInCurrentMinute = 0;
                }

                EmailLog logEntry = emailLogRepository.findById(emailLogId).orElseThrow();
                log.debug("Processing email for recipient: {}", logEntry.getRecipientEmail());

                String aiBody = logEntry.getBody();
                if (aiBody == null || aiBody.trim().isEmpty() || aiBody.equals("AI_GENERATED")) {
                    List<CvKeyword> studentKeywords = cvKeywordRepository.findByCvId(campaign.getCv().getId());
                    String keywordsStr = studentKeywords.stream().map(CvKeyword::getKeyword).collect(Collectors.joining(", "));
                    
                    aiBody = openRouterService.generateOutreachEmail(keywordsStr, 
                        logEntry.getProfessor().getFirstName() + " " + logEntry.getProfessor().getLastName(),
                        logEntry.getProfessor().getUniversity().getName(), logEntry.getMatchResult().getMatchedKeywords());
                }

                boolean success = self.sendAndUpdateLog(emailLogId, aiBody, attachment, fileName, mailSender, smtpAccount);
                if (success) {
                    sent++;
                } else {
                    failed++;
                }
            } catch (InterruptedException e) {
                log.error("Campaign processing interrupted", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Failed to process email log entry: " + emailLogId, e);
                failed++;
            }

            if (++processed % batchSize == 0) {
                log.info("Campaign {}: Processed {}/{} emails (Sent: {}, Failed: {})", campaignId, processed, emailLogIds.size(), sent, failed);
                self.updateCampaignProgress(campaignId, sent, failed);
            }
        }
        log.info("Completed batch processing for campaign {}. Final stats - Sent: {}, Failed: {}", campaignId, sent, failed);
        self.updateCampaignProgress(campaignId, sent, failed);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean sendAndUpdateLog(UUID emailLogId, String aiBody, byte[] attachment, String attachmentName, JavaMailSender mailSender, SmtpAccount smtpAccount) {
        EmailLog emailLog = emailLogRepository.findById(emailLogId).orElseThrow();
        int attempts = 0;
        Exception lastException = null;

        if (aiBody != null && !aiBody.trim().isEmpty()) {
            emailLog.setBody(aiBody);
        }

        while (attempts < retryAttempts) {
            try {
                attempts++;
                emailLog.setRetryCount(attempts - 1);
                
                log.debug("Attempt {} to send email to {}", attempts, emailLog.getRecipientEmail());
                
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(smtpAccount.getEmail(), smtpAccount.getFromName());
                helper.setTo(emailLog.getRecipientEmail());
                helper.setSubject(emailLog.getSubject());
                helper.setText(emailLog.getBody(), false);

                if (attachment != null) {
                    helper.addAttachment(attachmentName, new org.springframework.core.io.ByteArrayResource(attachment));
                }
                
                mailSender.send(message);
                
                emailLog.setStatus(EmailLog.EmailStatus.SENT);
                emailLog.setSentAt(LocalDateTime.now());
                emailLog.setErrorMessage(null);
                emailLogRepository.save(emailLog);
                
                log.info("Successfully sent email to {} on attempt {}", emailLog.getRecipientEmail(), attempts);
                return true;
            } catch (Exception e) {
                lastException = e;
                log.warn("Failed attempt {} to send email to {}: {}", attempts, emailLog.getRecipientEmail(), e.getMessage());
                if (attempts < retryAttempts) {
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("All {} attempts failed for email to {}", retryAttempts, emailLog.getRecipientEmail());
        emailLog.setStatus(EmailLog.EmailStatus.FAILED);
        emailLog.setErrorMessage(lastException != null ? lastException.getMessage() : "Unknown error");
        emailLogRepository.save(emailLog);
        return false;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateCampaignStatus(UUID campaignId, EmailCampaign.CampaignStatus status, LocalDateTime startedAt, LocalDateTime completedAt) {
        campaignRepository.findById(campaignId).ifPresent(c -> {
            c.setStatus(status);
            if (startedAt != null) c.setStartedAt(startedAt);
            if (completedAt != null) c.setCompletedAt(completedAt);
            campaignRepository.save(c);
        });
    }

    @Async
    public void prepareCampaignOptions(UUID campaignId) {
        log.info("Preparing AI email options for campaign: {}", campaignId);
        try {
            EmailCampaign campaign = campaignRepository.findById(campaignId).orElseThrow();
            List<EmailLog> logs = emailLogRepository.findByEmailCampaignId(campaignId);
            
            for (EmailLog logEntry : logs) {
                try {
                    List<CvKeyword> studentKeywords = cvKeywordRepository.findByCvId(campaign.getCv().getId());
                    String keywordsStr = studentKeywords.stream().map(CvKeyword::getKeyword).collect(Collectors.joining(", "));
                    
                    List<String> options = openRouterService.generateEmailOptions(keywordsStr, 
                        logEntry.getProfessor().getFirstName() + " " + logEntry.getProfessor().getLastName(),
                        logEntry.getProfessor().getUniversity().getName(), logEntry.getMatchResult().getMatchedKeywords(), 3);

                    if (!options.isEmpty()) {
                        self.saveEmailOptions(logEntry.getId(), options);
                    }
                } catch (Exception e) {
                    log.error("Failed to prepare options for log entry: " + logEntry.getId(), e);
                }
            }
            log.info("Completed AI email options preparation for campaign: {}", campaignId);
        } catch (Exception e) {
            log.error("Failed to prepare campaign options: " + campaignId, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveEmailOptions(UUID logId, List<String> options) {
        EmailLog logEntry = emailLogRepository.findById(logId).orElseThrow();
        logEntry.setBody(options.get(0)); // Set first option as default body
        
        for (int i = 0; i < options.size(); i++) {
            EmailOption option = EmailOption.builder()
                .emailLog(logEntry)
                .body(options.get(i))
                .isSelected(i == 0)
                .build();
            emailOptionRepository.save(option);
            logEntry.getOptions().add(option);
        }
        emailLogRepository.save(logEntry);
    }

    /**
     * Selects a specific AI-generated email option for a log entry.
     */
    @Transactional
    public void selectEmailOption(UUID logId, UUID optionId, UUID tenantId) {
        EmailLog logEntry = emailLogRepository.findById(logId).orElseThrow();
        if (!logEntry.getTenant().getId().equals(tenantId)) {
            throw new IllegalArgumentException("Access denied");
        }
        
        List<EmailOption> options = emailOptionRepository.findByEmailLogId(logId);
        boolean found = false;
        
        for (EmailOption opt : options) {
            if (opt.getId().equals(optionId)) {
                opt.setIsSelected(true);
                logEntry.setBody(opt.getBody());
                found = true;
            } else {
                opt.setIsSelected(false);
            }
            emailOptionRepository.save(opt);
        }
        
        if (!found) {
            throw new IllegalArgumentException("Option not found for this log");
        }
        emailLogRepository.save(logEntry);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<UUID> initializeEmailLogs(UUID campaignId, List<MatchResult> matches) {
        EmailCampaign campaign = campaignRepository.findById(campaignId).orElseThrow();
        List<EmailLog> logs = matches.stream().map(m -> createEmailLog(campaign, m)).collect(Collectors.toList());
        return emailLogRepository.saveAll(logs).stream().map(EmailLog::getId).collect(Collectors.toList());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateCampaignProgress(UUID campaignId, int sent, int failed) {
        campaignRepository.findById(campaignId).ifPresent(c -> {
            c.setSentCount(sent);
            c.setFailedCount(failed);
            campaignRepository.save(c);
        });
    }

    private EmailLog createEmailLog(EmailCampaign campaign, MatchResult match) {
        return EmailLog.builder()
            .tenant(campaign.getTenant()).emailCampaign(campaign).professor(match.getProfessor())
            .matchResult(match).recipientEmail(match.getProfessor().getEmail()).subject(campaign.getSubject())
            .body(substituteTemplate(campaign.getBodyTemplate(), match.getProfessor(), match))
            .status(EmailLog.EmailStatus.PENDING).build();
    }

    private String substituteTemplate(String t, Professor p, MatchResult m) {
        return t.replace("{{professor_name}}", p.getFirstName() + " " + p.getLastName())
                .replace("{{university}}", p.getUniversity().getName())
                .replace("{{matched_keywords}}", m.getMatchedKeywords());
    }

    /**
     * Schedules a campaign for execution.
     */
    @Transactional
    public void scheduleCampaign(UUID campaignId, UUID tenantId, LocalDateTime scheduledAt) {
        EmailCampaign campaign = getCampaign(campaignId, tenantId);
        if (campaign.getStatus() != EmailCampaign.CampaignStatus.DRAFT) throw new IllegalStateException("Only draft campaigns can be scheduled");
        campaign.setScheduledAt(scheduledAt);
        campaign.setStatus(EmailCampaign.CampaignStatus.SCHEDULED);
        campaignRepository.save(campaign);
    }

    /**
     * Cancels a scheduled campaign.
     */
    @Transactional
    public void cancelCampaign(UUID campaignId, UUID tenantId) {
        EmailCampaign campaign = getCampaign(campaignId, tenantId);
        if (campaign.getStatus() != EmailCampaign.CampaignStatus.SCHEDULED) throw new IllegalStateException("Only scheduled campaigns can be cancelled");
        campaign.setStatus(EmailCampaign.CampaignStatus.CANCELLED);
        campaignRepository.save(campaign);
    }

    @Transactional(readOnly = true)
    public EmailCampaign getCampaign(UUID id, UUID tid) { return campaignRepository.findByIdAndTenantId(id, tid).orElseThrow(); }
    
    @Transactional(readOnly = true)
    public Page<EmailCampaign> getAllCampaigns(UUID tid, Pageable p) { return campaignRepository.findAllByTenantId(tid, p); }

    @Transactional(readOnly = true)
    public Page<EmailLog> getCampaignLogs(UUID id, Pageable p) { return emailLogRepository.findByCampaignId(id, p); }
}
