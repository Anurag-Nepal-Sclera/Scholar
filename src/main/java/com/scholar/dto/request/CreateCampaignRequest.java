package com.scholar.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateCampaignRequest {
    
    @NotNull(message = "CV ID is required")
    private UUID cvId;

    @NotBlank(message = "Campaign name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Subject is required")
    @Size(max = 500, message = "Subject must not exceed 500 characters")
    private String subject;

    @NotBlank(message = "Body template is required")
    private String bodyTemplate;

    @NotNull(message = "Minimum match score is required")
    @DecimalMin(value = "0.0", message = "Score must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Score must be between 0 and 1")
    private BigDecimal minMatchScore;

    private java.util.List<UUID> matchIds;
}
