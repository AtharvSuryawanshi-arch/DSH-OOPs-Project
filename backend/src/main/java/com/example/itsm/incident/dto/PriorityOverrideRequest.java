package com.example.itsm.incident.dto;

import com.example.itsm.incident.Priority;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PriorityOverrideRequest(
        @NotNull(message = "priority is required")
        Priority priority,

        @Size(max = 500, message = "reason must be at most 500 characters")
        String reason
) {
}
