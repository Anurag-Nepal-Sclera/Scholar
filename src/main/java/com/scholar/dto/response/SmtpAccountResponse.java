package com.scholar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmtpAccountResponse {
    private UUID id;
    private String email;
    private String smtpHost;
    private Integer smtpPort;
    private String username;
    private Boolean useTls;
    private Boolean useSsl;
    private String fromName;
    private String status;
}
