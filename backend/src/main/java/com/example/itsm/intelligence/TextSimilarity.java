package com.example.itsm.intelligence;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class TextSimilarity {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "with", "from", "that", "this", "have", "has", "was", "were",
            "are", "is", "it", "its", "not", "can", "cannot", "unable", "our", "all", "any",
            "when", "while", "into", "over", "under", "after", "before", "been", "being",
            "will", "would", "could", "should", "does", "did", "doing", "to", "of", "in",
            "on", "at", "by", "as", "an", "a", "or", "we", "us", "they", "them", "their"
    );

    private TextSimilarity() {
    }

    public static Set<String> tokens(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        Set<String> tokens = new LinkedHashSet<>();
        Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .map(String::trim)
                .filter(token -> token.length() > 2)
                .filter(token -> !STOP_WORDS.contains(token))
                .forEach(tokens::add);
        return tokens;
    }

    public static double jaccard(String left, String right) {
        return jaccard(tokens(left), tokens(right));
    }

    public static double jaccard(Set<String> left, Set<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }
        long intersection = left.stream().filter(right::contains).count();
        long union = left.size() + right.size() - intersection;
        return union == 0 ? 0.0 : (double) intersection / union;
    }

    public static double combinedSimilarity(String leftTitle, String leftBody, String rightTitle, String rightBody) {
        double titleScore = jaccard(leftTitle, rightTitle);
        double bodyScore = jaccard(leftBody, rightBody);
        return (titleScore * 0.55) + (bodyScore * 0.45);
    }

    public static double incidentSimilarity(
            String leftTitle,
            String leftBody,
            String rightTitle,
            String rightBody
    ) {
        double titleScore = overlap(leftTitle, rightTitle);
        double bodyScore = overlap(leftBody, rightBody);
        return (titleScore * 0.6) + (bodyScore * 0.4);
    }

    public static double overlap(String left, String right) {
        return overlap(tokens(left), tokens(right));
    }

    public static double overlap(Set<String> left, Set<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }
        long intersection = left.stream().filter(right::contains).count();
        return (double) intersection / Math.min(left.size(), right.size());
    }
}
