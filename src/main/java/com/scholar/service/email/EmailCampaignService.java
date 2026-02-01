package com.scholar.service.email;

import org.springframework.transaction.support.TransactionSynchronizationManager;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
        this.self = self;
    }

    @Value("${scholar.email.batch-size}")
    private int batchSize;

    @Value("${scholar.email.rate-limit-per-minute}")
    private int rateLimitPerMinute;

    /**
     * Creates a new email campaign.
     */
    @Transactional
    public EmailCampaign createCampaign(UUID tenantId, UUID cvId, String name, String subject, String bodyTemplate, BigDecimal minMatchScore) {
        // Handle optional SMTP account
        SmtpAccount smtpAccount = smtpAccountService.findActiveSmtpAccount(tenantId).orElse(null);
        
        if (smtpAccount == null) {
            log.warn("No active SMTP account found for tenant {}. Campaign will be created without SMTP config.", tenantId);
        }

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
        BigDecimal autoThreshold = new BigDecimal("0.40"); // Default min score
        String campaignName = "AI-Outreach: " + cv.getOriginalFilename() + " (" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) + ")";
        
        EmailCampaign campaign = createCampaign(tenantId, cvId, campaignName, "Research Inquiry: " + cv.getOriginalFilename(), "AI_GENERATED", autoThreshold);
        
        // Initialize logs with TOP 50 matches (instead of top 5 or all \u003e threshold)
        Pageable top50 = PageRequest.of(0, 50);
        List<MatchResult> matches = matchResultRepository.findByCvIdAndTenantIdOrderByScoreDesc(cvId, tenantId, top50).getContent();
        
        // Update campaign recipient count to reflect actual selected matches
        campaign.setTotalRecipients(matches.size());
        campaignRepository.save(campaign);
        
        if (!matches.isEmpty()) {
            List<UUID> logIds = initializeEmailLogs(campaign.getId(), matches);
            
            // Log for debugging
            log.info("Initialized {} email logs for campaign {}. Triggering draft generation...", logIds.size(), campaign.getId());
            
            // Trigger async generation of email drafts
            // Using a lambda or ensuring 'self' proxy is used correctly to spawn async task
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        self.generateDraftsForCampaign(campaign.getId(), logIds);
                    }
                });
            } else {
                self.generateDraftsForCampaign(campaign.getId(), logIds);
            }
        } else {
            log.info("No matches found for auto-campaign. Campaign created but empty.");
        }
    }

    @Async
    @Transactional
    public void generateDraftsForCampaign(UUID campaignId, List<UUID> emailLogIds) {
        log.info("Starting async email draft generation for campaign: {}", campaignId);
        try {
            // Need to reload entities in new transaction context
            EmailCampaign campaign = campaignRepository.findById(campaignId).orElseThrow();
            List<CvKeyword> studentKeywords = cvKeywordRepository.findByCvId(campaign.getCv().getId());
            String keywordsStr = studentKeywords.stream().map(CvKeyword::getKeyword).collect(Collectors.joining(", "));

            for (UUID logId : emailLogIds) {
                try {
                    EmailLog emailLog = emailLogRepository.findById(logId).orElseThrow();
                    
                    // Skip if already generated (unless it was a placeholder)
                    if (emailLog.getBody() != null && !emailLog.getBody().isEmpty() && !emailLog.getBody().equals("AI_GENERATED")) {
                        continue; 
                    }

                    Professor professor = emailLog.getProfessor();
                    
                    List<String> options = openRouterService.generateOutreachEmailOptions(
                        keywordsStr,
                        professor.getFirstName() + " " + professor.getLastName(),
                        professor.getUniversity().getName(),
                        emailLog.getMatchResult().getMatchedKeywords(),
                        professor.getPublications() // Pass publications here!
                    );

                    if (!options.isEmpty()) {
                        emailLog.setBody(options.get(0));
                        emailLog.setAlternateBodies(String.join("###SPLIT###", options));
                        emailLogRepository.save(emailLog);
                        log.debug("Generated and saved email draft for log: {}", logId);
                    }
                } catch (Exception e) {
                    log.error("Failed to generate draft for log: {}", logId, e);
                }
            }
            log.info("Completed email draft generation for campaign: {}", campaignId);
        } catch (Exception e) {
            log.error("Fatal error in draft generation task for campaign: {}", campaignId, e);
        }
    }

    @Async
    @Transactional
    public void executeCampaign(UUID campaignId) {
        try {
            EmailCampaign campaign = campaignRepository.findById(campaignId).orElseThrow();
            // Allow execution if SCHEDULED or if manually triggered (IN_PROGRESS check handled by caller or status update)
            // if (campaign.getStatus() != EmailCampaign.CampaignStatus.SCHEDULED && campaign.getStatus() != EmailCampaign.CampaignStatus.DRAFT) return;

            byte[] cvFile = fileStorageService.retrieveFile(campaign.getCv().getFilePath());
            String fileName = campaign.getCv().getOriginalFilename();

            self.updateCampaignStatus(campaignId, EmailCampaign.CampaignStatus.IN_PROGRESS, LocalDateTime.now(), null);

            List<MatchResult> matches = matchResultRepository.findByCvIdAndTenantIdAndMinScore(campaign.getCv().getId(), campaign.getTenant().getId(), campaign.getMinMatchScore());
            List<MatchResult> validMatches = matches.stream()
                .filter(match -> !blacklistRepository.isBlacklisted(match.getProfessor().getEmail(), campaign.getTenant().getId()))
                .collect(Collectors.toList());

            List<UUID> emailLogIds = self.initializeEmailLogs(campaignId, validMatches);
            processBatchedEmails(campaignId, emailLogIds, cvFile, fileName);

            self.updateCampaignStatus(campaignId, EmailCampaign.CampaignStatus.COMPLETED, null, LocalDateTime.now());
        } catch (Exception e) {
            log.error("Campaign failed", e);
            self.updateCampaignStatus(campaignId, EmailCampaign.CampaignStatus.FAILED, null, null);
        }
    }

    private void processBatchedEmails(UUID campaignId, List<UUID> emailLogIds, byte[] attachment, String fileName) {
        EmailCampaign campaign = campaignRepository.findById(campaignId).orElseThrow();
        SmtpAccount smtpAccount = campaign.getSmtpAccount();
        JavaMailSender mailSender = createMailSender(smtpAccount);

        int processed = 0, sent = 0, failed = 0;
        long batchStartTime = System.currentTimeMillis();
        int emailsInCurrentMinute = 0;

        for (UUID emailLogId : emailLogIds) {
            try {
                if (++emailsInCurrentMinute >= rateLimitPerMinute) {
                    long waitTime = 60000 - (System.currentTimeMillis() - batchStartTime);
                    if (waitTime > 0) Thread.sleep(waitTime);
                    batchStartTime = System.currentTimeMillis();
                    emailsInCurrentMinute = 0;
                }

                EmailLog logEntry = emailLogRepository.findById(emailLogId).orElseThrow();
                List<CvKeyword> studentKeywords = cvKeywordRepository.findByCvId(campaign.getCv().getId());
                String keywordsStr = studentKeywords.stream().map(CvKeyword::getKeyword).collect(Collectors.joining(", "));
                
                String aiBody = openRouterService.generateOutreachEmail(keywordsStr, 
                    logEntry.getProfessor().getFirstName() + " " + logEntry.getProfessor().getLastName(),
                    logEntry.getProfessor().getUniversity().getName(), logEntry.getMatchResult().getMatchedKeywords());

                boolean success = self.sendAndUpdateLog(emailLogId, aiBody, attachment, fileName, mailSender, smtpAccount);
                if (success) sent++; else failed++;
            } catch (Exception e) { failed++; }

            if (++processed % batchSize == 0) self.updateCampaignProgress(campaignId, sent, failed);
        }
        self.updateCampaignProgress(campaignId, sent, failed);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean sendAndUpdateLog(UUID emailLogId, String aiBody, byte[] attachment, String attachmentName, JavaMailSender mailSender, SmtpAccount smtpAccount) {
        EmailLog emailLog = emailLogRepository.findById(emailLogId).orElseThrow();
        try {
            if (aiBody != null && !aiBody.trim().isEmpty()) emailLog.setBody(aiBody);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(smtpAccount.getEmail(), smtpAccount.getFromName());
            helper.setTo(emailLog.getRecipientEmail());
            helper.setSubject(emailLog.getSubject());
            helper.setText(emailLog.getBody(), false);

            if (attachment != null) helper.addAttachment(attachmentName, new org.springframework.core.io.ByteArrayResource(attachment));
            mailSender.send(message);
            
            emailLog.setStatus(EmailLog.EmailStatus.SENT);
            emailLog.setSentAt(LocalDateTime.now());
            emailLogRepository.save(emailLog);
            return true;
        } catch (Exception e) {
            emailLog.setStatus(EmailLog.EmailStatus.FAILED);
            emailLog.setErrorMessage(e.getMessage());
            emailLogRepository.save(emailLog);
            return false;
        }
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<UUID> initializeEmailLogs(UUID campaignId, List<MatchResult> matches) {
        EmailCampaign campaign = campaignRepository.findById(campaignId).orElseThrow();
        
        List<EmailLog> logsToSave = new ArrayList<>();
        Set<UUID> processedProfessorIds = new HashSet<>();

        for (MatchResult match : matches) {
            Professor professor = match.getProfessor();
            // 1. Deduplication within the current batch
            if (processedProfessorIds.contains(professor.getId())) {
                continue; 
            }
            
            // 2. Check existence in DB to avoid unique constraint violation
            boolean exists = emailLogRepository.existsByEmailCampaignIdAndProfessorId(campaignId, professor.getId());
            if (!exists) {
                logsToSave.add(createEmailLog(campaign, match));
                processedProfessorIds.add(professor.getId());
            }
        }

        if (logsToSave.isEmpty()) {
            return List.of();
        }

        return emailLogRepository.saveAll(logsToSave).stream().map(EmailLog::getId).collect(Collectors.toList());
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

    private JavaMailSender createMailSender(SmtpAccount s) {
        JavaMailSenderImpl ms = new JavaMailSenderImpl();
        ms.setHost(s.getSmtpHost()); ms.setPort(s.getSmtpPort());
        ms.setUsername(s.getUsername()); ms.setPassword(smtpAccountService.decryptPassword(s));
        Properties p = ms.getJavaMailProperties();
        p.put("mail.smtp.auth", "true");
        if (s.getUseTls()) { p.put("mail.smtp.starttls.enable", "true"); p.put("mail.smtp.starttls.required", "true"); }
        if (s.getUseSsl()) p.put("mail.smtp.ssl.enable", "true");
        return ms;
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
    public EmailLog getEmailLog(UUID logId) {
        return emailLogRepository.findById(logId)
            .orElseThrow(() -> new IllegalArgumentException("Email log not found: " + logId));
    }

    @Transactional
    public EmailLog updateEmailLogBody(UUID logId, String body, UUID tenantId) {
        EmailLog emailLog = getEmailLog(logId);
        if (!emailLog.getTenant().getId().equals(tenantId)) {
            throw new SecurityException("Access denied");
        }
        emailLog.setBody(body);
        return emailLogRepository.save(emailLog);
    }

    @Transactional
    public EmailLog regenerateEmailLog(UUID logId, UUID tenantId) {
        EmailLog emailLog = getEmailLog(logId);
        if (!emailLog.getTenant().getId().equals(tenantId)) {
            throw new SecurityException("Access denied");
        }
        
        EmailCampaign campaign = emailLog.getEmailCampaign();
        List<CvKeyword> studentKeywords = cvKeywordRepository.findByCvId(campaign.getCv().getId());
        String keywordsStr = studentKeywords.stream().map(CvKeyword::getKeyword).collect(Collectors.joining(", "));
        Professor professor = emailLog.getProfessor();

        List<String> options = openRouterService.generateOutreachEmailOptions(
            keywordsStr,
            professor.getFirstName() + " " + professor.getLastName(),
            professor.getUniversity().getName(),
            emailLog.getMatchResult().getMatchedKeywords(),
            professor.getPublications()
        );

        if (!options.isEmpty()) {
            emailLog.setBody(options.get(0));
            emailLog.setAlternateBodies(String.join("###SPLIT###", options));
            return emailLogRepository.save(emailLog);
        }
        
        throw new RuntimeException("Failed to regenerate email");
    }

    @Transactional
    public void sendIndividualEmail(UUID logId, UUID tenantId) {
        EmailLog emailLog = getEmailLog(logId);
        if (!emailLog.getTenant().getId().equals(tenantId)) {
            throw new SecurityException("Access denied");
        }
        
        EmailCampaign campaign = emailLog.getEmailCampaign();
        byte[] cvFile;
        try {
            cvFile = fileStorageService.retrieveFile(campaign.getCv().getFilePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve CV file", e);
        }
        String fileName = campaign.getCv().getOriginalFilename();
        
        SmtpAccount smtpAccount = campaign.getSmtpAccount();
        JavaMailSender mailSender = createMailSender(smtpAccount);
        
        boolean success = sendAndUpdateLog(logId, emailLog.getBody(), cvFile, fileName, mailSender, smtpAccount);
        if (!success) {
            throw new RuntimeException("Failed to send email");
        }
        
        // Update campaign counters
        campaign.setSentCount(campaign.getSentCount() + 1);
        campaignRepository.save(campaign);
    }

    @Transactional(readOnly = true)
    public Page<EmailLog> getCampaignLogs(UUID id, Pageable p) { return emailLogRepository.findByCampaignId(id, p); }

    @Transactional(readOnly = true)
    public Page<EmailLog> getAllTenantLogs(UUID tenantId, Pageable p) { return emailLogRepository.findByTenantId(tenantId, p); }

    /**
     * Deletes a campaign and all associated email logs (cascade delete).
     */
    @Transactional
    public void deleteCampaign(UUID campaignId, UUID tenantId) {
        EmailCampaign campaign = getCampaign(campaignId, tenantId);
        
        // Only allow deletion of campaigns that are not currently executing
        if (campaign.getStatus() == EmailCampaign.CampaignStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot delete a campaign that is currently in progress");
        }
        
        // Email logs will be cascade deleted automatically due to CascadeType.ALL on the entity
        campaignRepository.delete(campaign);
        log.info("Deleted campaign {} and its associated email logs for tenant {}", campaignId, tenantId);
    }
}
