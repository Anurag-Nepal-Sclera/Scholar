package com.scholar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CVResponse {
    private UUID id;
    private String originalFilename;
    private Long fileSizeBytes;
    private String mimeType;
    private String parsingStatus;
    private LocalDateTime parsedAt;
    private LocalDateTime uploadedAt;
    private Integer keywordCount;
}
