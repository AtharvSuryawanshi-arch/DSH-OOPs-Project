package com.example.itsm.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "code is required")
        @Size(max = 40, message = "code must be at most 40 characters")
        String code,

        @NotBlank(message = "name is required")
        @Size(max = 80, message = "name must be at most 80 characters")
        String name,

        @Size(max = 255, message = "description must be at most 255 characters")
        String description,

        Boolean active
) {
}
