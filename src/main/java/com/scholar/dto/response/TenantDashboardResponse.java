package com.scholar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDashboardResponse {
    private long totalCvs;
    private long totalMatches;
    private long totalCampaigns;
    private long totalEmailsSent;
    private long totalEmailsFailed;
    private boolean smtpConfigured;
    private Map<String, Long> campaignStatusCounts;
}
