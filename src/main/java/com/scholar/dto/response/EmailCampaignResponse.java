package com.scholar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailCampaignResponse {
    private UUID id;
    private String name;
    private String subject;
    private BigDecimal minMatchScore;
    private String status;
    private Integer totalRecipients;
    private Integer sentCount;
    private Integer failedCount;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
