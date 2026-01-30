package com.scholar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomingEmailResponse {
    private String from;
    private String subject;
    private String bodyPreview;
    private LocalDateTime receivedDate;
}
