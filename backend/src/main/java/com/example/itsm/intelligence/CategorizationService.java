package com.example.itsm.intelligence;

import com.example.itsm.catalog.Category;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CategorizationService {

    private static final Map<String, List<String>> KEYWORDS = keywordMap();

    public ClassificationSuggestion suggest(String title, String description, List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return new ClassificationSuggestion(null, null, 0.0, "No categories are configured.");
        }

        String text = ((title == null ? "" : title) + " " + (description == null ? "" : description))
                .toLowerCase(Locale.ROOT);

        Map<Category, Integer> scores = new LinkedHashMap<>();
        for (Category category : categories) {
            int score = 0;
            for (String keyword : KEYWORDS.getOrDefault(category.getCode(), List.of())) {
                if (text.contains(keyword)) {
                    score++;
                    if (title != null && title.toLowerCase(Locale.ROOT).contains(keyword)) {
                        score += 2;
                    }
                }
            }
            scores.put(category, score);
        }

        Map.Entry<Category, Integer> best = scores.entrySet().stream()
                .max(Comparator.<Map.Entry<Category, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparing(entry -> entry.getKey().getName()))
                .orElseThrow();

        if (best.getValue() == 0) {
            Category fallback = categories.stream()
                    .filter(category -> "OTHER".equalsIgnoreCase(category.getCode()))
                    .findFirst()
                    .orElse(categories.getFirst());
            return new ClassificationSuggestion(
                    fallback.getId(),
                    fallback.getName(),
                    0.3,
                    "No strong keyword match; routed to " + fallback.getName() + " for manual review."
            );
        }

        double confidence = Math.min(0.96, 0.5 + best.getValue() * 0.075);
        return new ClassificationSuggestion(
                best.getKey().getId(),
                best.getKey().getName(),
                Math.round(confidence * 100.0) / 100.0,
                "Matched %d keyword signal(s) for %s.".formatted(best.getValue(), best.getKey().getName())
        );
    }

    private static Map<String, List<String>> keywordMap() {
        Map<String, List<String>> keywords = new LinkedHashMap<>();
        keywords.put("HARDWARE", List.of("laptop", "monitor", "keyboard", "mouse", "printer", "device", "battery", "screen", "dock"));
        keywords.put("SOFTWARE", List.of("install", "update", "license", "bug", "software", "version", "patch", "crash"));
        keywords.put("NETWORK", List.of("network", "vpn", "wifi", "latency", "packet", "dns", "connectivity", "internet", "switch", "router", "timeout"));
        keywords.put("DATABASE", List.of("database", "sql", "query", "deadlock", "replication", "postgres", "postgresql", "oracle", "table", "backup", "storage engine"));
        keywords.put("CLOUD", List.of("cloud", "aws", "azure", "gcp", "kubernetes", "container", "bucket", "s3", "instance"));
        keywords.put("SECURITY", List.of("security", "phishing", "malware", "ransomware", "breach", "suspicious", "virus", "unauthorized", "certificate"));
        keywords.put("AUTHENTICATION", List.of("login", "password", "sso", "authentication", "mfa", "access denied", "account locked", "token", "sign in"));
        keywords.put("APPLICATION", List.of("payment", "portal", "app", "application", "error", "unavailable", "transaction", "checkout", "dashboard", "workflow"));
        keywords.put("STORAGE", List.of("disk", "space", "volume", "file share", "nas", "full", "storage", "quota"));
        return Map.copyOf(keywords);
    }
}
