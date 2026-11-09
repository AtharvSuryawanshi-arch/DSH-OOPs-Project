package com.example.itsm.incident.dto;

import jakarta.validation.constraints.Size;

public record CloseIncidentRequest(
        @Size(max = 5000, message = "closure notes must be at most 5000 characters")
        String closureNotes
) {
}
