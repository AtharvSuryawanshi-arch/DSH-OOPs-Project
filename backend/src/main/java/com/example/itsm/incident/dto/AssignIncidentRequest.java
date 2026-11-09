package com.example.itsm.incident.dto;

import jakarta.validation.constraints.NotNull;

public record AssignIncidentRequest(
        @NotNull(message = "technicianId is required")
        Long technicianId
) {
}
