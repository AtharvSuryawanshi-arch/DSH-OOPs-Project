package com.example.itsm.sla;

import com.example.itsm.incident.Priority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SlaPolicyRequest(
        @NotBlank(message = "name is required")
        @Size(max = 120, message = "name must be at most 120 characters")
        String name,

        @NotNull(message = "priority is required")
        Priority priority,

        Long serviceId,

        @Min(value = 1, message = "response target must be at least 1 minute")
        int responseMinutes,

        @Min(value = 1, message = "resolution target must be at least 1 minute")
        int resolutionMinutes,

        @Min(value = 1, message = "warning percent must be at least 1")
        @Max(value = 99, message = "warning percent must be at most 99")
        int warningPercent,

        Boolean active
) {
}
