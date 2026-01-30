package com.scholar.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SmtpAccountRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "SMTP host is required")
    private String smtpHost;

    @NotNull(message = "SMTP port is required")
    @Min(value = 1, message = "Port must be between 1 and 65535")
    @Max(value = 65535, message = "Port must be between 1 and 65535")
    private Integer smtpPort;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    private Boolean useTls;
    private Boolean useSsl;
    private String fromName;
}
