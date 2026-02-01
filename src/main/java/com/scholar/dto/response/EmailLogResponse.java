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
public class EmailLogResponse {
    private UUID id;
    private String recipientEmail;
    private String subject;
    private String body;
    private String alternateBodies;
    private String status;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private UUID professorId;
    private String professorName;
}
