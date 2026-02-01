package com.scholar.service.email;

import com.scholar.domain.entity.EmailCampaign;
import com.scholar.domain.repository.EmailCampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler for executing scheduled email campaigns.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailCampaignScheduler {

    private final EmailCampaignRepository campaignRepository;
    private final EmailCampaignService campaignService;

    /**
     * Checks for scheduled campaigns every minute and triggers execution.
     */
    @Scheduled(fixedRate = 60000)
    public void processScheduledCampaigns() {
        log.debug("Checking for scheduled campaigns at {}", LocalDateTime.now());
        List<EmailCampaign> scheduledCampaigns = campaignRepository.findScheduledCampaigns(
            EmailCampaign.CampaignStatus.SCHEDULED, 
            LocalDateTime.now()
        );

        if (!scheduledCampaigns.isEmpty()) {
            log.info("Found {} campaigns to execute", scheduledCampaigns.size());
            for (EmailCampaign campaign : scheduledCampaigns) {
                try {
                    log.info("Triggering campaign: {} ({})", campaign.getName(), campaign.getId());
                    campaignService.executeCampaign(campaign.getId());
                } catch (Exception e) {
                    log.error("Failed to trigger campaign: " + campaign.getId(), e);
                }
            }
        }
    }
}
