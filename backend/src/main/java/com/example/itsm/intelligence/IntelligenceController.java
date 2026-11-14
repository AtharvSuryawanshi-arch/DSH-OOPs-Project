package com.example.itsm.intelligence;

import com.example.itsm.catalog.Category;
import com.example.itsm.catalog.CategoryRepository;
import com.example.itsm.incident.IncidentAccessPolicy;
import com.example.itsm.incident.dto.DuplicateResponse;
import com.example.itsm.security.CurrentUserService;
import com.example.itsm.security.UserPrincipal;
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
    private final CurrentUserService currentUserService;
    private final IncidentAccessPolicy accessPolicy;

    public IntelligenceController(
            CategorizationService categorizationService,
            DuplicateDetectionService duplicateDetectionService,
            CategoryRepository categoryRepository,
            CurrentUserService currentUserService,
            IncidentAccessPolicy accessPolicy
    ) {
        this.categorizationService = categorizationService;
        this.duplicateDetectionService = duplicateDetectionService;
        this.categoryRepository = categoryRepository;
        this.currentUserService = currentUserService;
        this.accessPolicy = accessPolicy;
    }

    /** Preview shown on the report form: suggested category and possible duplicates. */
    @PostMapping("/classify")
    public IntelligencePreview classify(@Valid @RequestBody IntelligencePreview.ClassifyRequest request) {
        List<Category> categories = categoryRepository.findByActiveTrueOrderByNameAsc();
        ClassificationSuggestion suggestion = categorizationService.suggest(
                request.title(),
                request.description(),
                categories
        );
        UserPrincipal viewer = currentUserService.requirePrincipal();
        // Someone else's similar report is mentioned, but without its title, unless the viewer may open it.
        List<DuplicateResponse> duplicates = duplicateDetectionService.findMatches(
                        request.title(),
                        request.description(),
                        request.serviceId(),
                        null
                ).stream()
                .map(match -> DuplicateResponse.of(match, accessPolicy.canView(match.incident(), viewer)))
                .toList();
        return new IntelligencePreview(suggestion, duplicates);
    }
}
