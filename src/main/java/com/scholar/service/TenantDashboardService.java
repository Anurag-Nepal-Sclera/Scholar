package com.scholar.service;

import com.scholar.domain.entity.EmailCampaign;
import com.scholar.domain.entity.EmailLog;
import com.scholar.domain.repository.*;
import com.scholar.dto.response.TenantDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantDashboardService {

    private final CVRepository cvRepository;
    private final MatchResultRepository matchResultRepository;
    private final EmailCampaignRepository campaignRepository;
    private final EmailLogRepository emailLogRepository;
    private final SmtpAccountRepository smtpAccountRepository;

    @Transactional(readOnly = true)
    public TenantDashboardResponse getDashboardStats(UUID tenantId) {
        long totalCvs = cvRepository.countByTenantId(tenantId);
        long totalMatches = matchResultRepository.countByTenantId(tenantId);
        long totalCampaigns = campaignRepository.countByTenantId(tenantId);
        long totalEmailsSent = emailLogRepository.countByTenantIdAndStatus(tenantId, EmailLog.EmailStatus.SENT);
        long totalEmailsFailed = emailLogRepository.countByTenantIdAndStatus(tenantId, EmailLog.EmailStatus.FAILED);
        boolean smtpConfigured = smtpAccountRepository.findByTenantId(tenantId).isPresent();

        Map<String, Long> campaignStatusCounts = campaignRepository.findAllByTenantId(tenantId, org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream()
                .collect(Collectors.groupingBy(c -> c.getStatus().name(), Collectors.counting()));

        return TenantDashboardResponse.builder()
                .totalCvs(totalCvs)
                .totalMatches(totalMatches)
                .totalCampaigns(totalCampaigns)
                .totalEmailsSent(totalEmailsSent)
                .totalEmailsFailed(totalEmailsFailed)
                .smtpConfigured(smtpConfigured)
                .campaignStatusCounts(campaignStatusCounts)
                .build();
    }
}
