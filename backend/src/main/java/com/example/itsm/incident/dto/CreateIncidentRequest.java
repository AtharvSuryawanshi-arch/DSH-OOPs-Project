package com.example.itsm.incident.dto;

import com.example.itsm.incident.Impact;
import com.example.itsm.incident.Urgency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIncidentRequest(
        @NotBlank(message = "title is required")
        @Size(max = 200, message = "title must be at most 200 characters")
        String title,

        @NotBlank(message = "description is required")
        @Size(max = 10000, message = "description must be at most 10000 characters")
        String description,

        @NotNull(message = "serviceId is required")
        Long serviceId,

        Long categoryId,

        @NotNull(message = "impact is required")
        Impact impact,

        @NotNull(message = "urgency is required")
        Urgency urgency,

        @Min(value = 1, message = "affected users must be at least 1")
        @Max(value = 1000000, message = "affected users is too large")
        int affectedUsers
) {
}
