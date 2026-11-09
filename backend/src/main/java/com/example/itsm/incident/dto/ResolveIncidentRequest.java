package com.example.itsm.incident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveIncidentRequest(
        @NotBlank(message = "resolution notes are required")
        @Size(max = 5000, message = "resolution notes must be at most 5000 characters")
        String resolutionNotes
) {
}
