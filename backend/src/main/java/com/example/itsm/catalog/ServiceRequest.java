package com.example.itsm.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServiceRequest(
        @NotBlank(message = "code is required")
        @Size(max = 40, message = "code must be at most 40 characters")
        String code,

        @NotBlank(message = "name is required")
        @Size(max = 120, message = "name must be at most 120 characters")
        String name,

        @Size(max = 500, message = "description must be at most 500 characters")
        String description,

        @NotNull(message = "business criticality is required")
        BusinessCriticality businessCriticality,

        Boolean active
) {
}
