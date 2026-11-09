package com.example.itsm.intelligence;

import com.example.itsm.incident.dto.DuplicateResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record IntelligencePreview(
        ClassificationSuggestion classification,
        List<DuplicateResponse> possibleDuplicates
) {

    public record ClassifyRequest(
            @NotBlank(message = "title is required")
            @Size(max = 200, message = "title must be at most 200 characters")
            String title,

            @NotBlank(message = "description is required")
            @Size(max = 10000, message = "description must be at most 10000 characters")
            String description,

            Long serviceId
    ) {
    }
}
