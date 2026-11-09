package com.example.itsm.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank(message = "name is required")
        @Size(max = 120, message = "name must be at most 120 characters")
        String name,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 160, message = "email must be at most 160 characters")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters")
        String password,

        @NotNull(message = "role is required")
        Role role,

        @Size(max = 40, message = "phone must be at most 40 characters")
        String phone,

        @Size(max = 120, message = "department must be at most 120 characters")
        String department,

        @Size(max = 120, message = "job title must be at most 120 characters")
        String jobTitle,

        @Min(value = 0, message = "experience must be zero or more")
        @Max(value = 60, message = "experience must be at most 60")
        Integer experienceYears,

        @Min(value = 1, message = "capacity must be at least 1")
        @Max(value = 50, message = "capacity must be at most 50")
        Integer maxCapacity,

        Set<Long> skillCategoryIds
) {
}
