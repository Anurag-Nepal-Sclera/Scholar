package com.scholar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResultResponse {
    private UUID id;
    private ProfessorSummary professor;
    private BigDecimal matchScore;
    private String matchedKeywords;
    private Integer totalCvKeywords;
    private Integer totalProfessorKeywords;
    private Integer totalMatchedKeywords;
    private List<EmailOptionResponse> emailOptions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfessorSummary {
        private UUID id;
        private String firstName;
        private String lastName;
        private String email;
        private String department;
        private String universityName;
        private String universityCountry;
    }
}
