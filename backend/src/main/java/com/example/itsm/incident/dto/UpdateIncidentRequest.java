package com.example.itsm.incident.dto;

import com.example.itsm.incident.Impact;
import com.example.itsm.incident.Urgency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateIncidentRequest(
        @Size(max = 200, message = "title must be at most 200 characters")
        String title,

        @Size(max = 10000, message = "description must be at most 10000 characters")
        String description,

        Impact impact,

        Urgency urgency,

        @Min(value = 1, message = "affected users must be at least 1")
        @Max(value = 1000000, message = "affected users is too large")
        Integer affectedUsers
) {
}
