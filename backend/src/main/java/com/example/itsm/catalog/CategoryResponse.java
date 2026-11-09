package com.example.itsm.catalog;

import java.time.Instant;

public record CategoryResponse(
        Long id,
        String code,
        String name,
        String description,
        boolean active,
        Instant createdAt
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt()
        );
    }
}
