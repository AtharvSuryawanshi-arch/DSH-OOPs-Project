package com.example.itsm.intelligence;

import com.example.itsm.catalog.Category;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CategorizationServiceTest {

    private final CategorizationService service = new CategorizationService();
    private final List<Category> categories = List.of(
            new Category("NETWORK", "Network", "Network"),
            new Category("AUTHENTICATION", "Authentication", "Auth"),
            new Category("DATABASE", "Database", "Database"),
            new Category("OTHER", "Other", "Other")
    );

    @Test
    void classifiesVpnAndWifiDescriptionAsNetwork() {
        ClassificationSuggestion suggestion = service.suggest(
                "VPN keeps disconnecting",
                "The corporate Wi-Fi network drops every few minutes and DNS lookups fail.",
                categories
        );

        assertThat(suggestion.categoryName()).isEqualTo("Network");
        assertThat(suggestion.confidence()).isGreaterThan(0.5);
    }

    @Test
    void classifiesLoginDescriptionAsAuthentication() {
        ClassificationSuggestion suggestion = service.suggest(
                "Cannot log in to SSO",
                "Password reset succeeds but the account is locked after MFA.",
                categories
        );

        assertThat(suggestion.categoryName()).isEqualTo("Authentication");
    }

    @Test
    void fallsBackToOtherWhenNoKeywordMatches() {
        ClassificationSuggestion suggestion = service.suggest(
                "General request",
                "Something unusual happened and I need help.",
                categories
        );

        assertThat(suggestion.categoryName()).isEqualTo("Other");
        assertThat(suggestion.confidence()).isEqualTo(0.3);
        assertThat(suggestion.rationale()).contains("manual review");
    }
}
