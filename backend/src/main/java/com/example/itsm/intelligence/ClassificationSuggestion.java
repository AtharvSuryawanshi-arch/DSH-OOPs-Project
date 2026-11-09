package com.example.itsm.intelligence;

public record ClassificationSuggestion(
        Long categoryId,
        String categoryName,
        double confidence,
        String rationale
) {
}
