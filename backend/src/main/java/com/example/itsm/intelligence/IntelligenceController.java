package com.example.itsm.intelligence;

import com.example.itsm.catalog.Category;
import com.example.itsm.catalog.CategoryRepository;
import com.example.itsm.incident.dto.DuplicateResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/intelligence")
public class IntelligenceController {

    private final CategorizationService categorizationService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final CategoryRepository categoryRepository;

    public IntelligenceController(
            CategorizationService categorizationService,
            DuplicateDetectionService duplicateDetectionService,
            CategoryRepository categoryRepository
    ) {
        this.categorizationService = categorizationService;
        this.duplicateDetectionService = duplicateDetectionService;
        this.categoryRepository = categoryRepository;
    }

    @PostMapping("/classify")
    public IntelligencePreview classify(@Valid @RequestBody IntelligencePreview.ClassifyRequest request) {
        List<Category> categories = categoryRepository.findByActiveTrueOrderByNameAsc();
        ClassificationSuggestion suggestion = categorizationService.suggest(
                request.title(),
                request.description(),
                categories
        );
        List<DuplicateResponse> duplicates = duplicateDetectionService.findMatches(
                        request.title(),
                        request.description(),
                        request.serviceId(),
                        null
                ).stream()
                .map(match -> new DuplicateResponse(
                        match.incident().getId(),
                        match.incident().getReference(),
                        match.incident().getTitle(),
                        match.incident().getStatus().name(),
                        match.incident().getPriority().name(),
                        match.score(),
                        match.incident().getCreatedAt()
                ))
                .toList();
        return new IntelligencePreview(suggestion, duplicates);
    }
}
