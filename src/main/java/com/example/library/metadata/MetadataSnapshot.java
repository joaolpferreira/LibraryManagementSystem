package com.example.library.metadata;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public record MetadataSnapshot(
        String publisher,
        Integer publishedYear,
        List<String> subjects,
        String coverUrl,
        String source,
        String sourceUrl
) {
    private static final int MAX_PUBLISHER_LENGTH = 200;
    private static final int MAX_SUBJECT_LENGTH = 200;
    private static final int MAX_SUBJECTS = 20;
    private static final int MAX_URL_LENGTH = 500;
    private static final int MAX_SOURCE_LENGTH = 50;

    public MetadataSnapshot {
        publisher = text(publisher, MAX_PUBLISHER_LENGTH);
        publishedYear = publishedYear != null
                && publishedYear >= 1000
                && publishedYear <= 9999
                ? publishedYear
                : null;
        subjects = subjects(subjects);
        coverUrl = value(coverUrl, MAX_URL_LENGTH);
        source = text(source, MAX_SOURCE_LENGTH);
        sourceUrl = value(sourceUrl, MAX_URL_LENGTH);
        if (source == null) {
            throw new IllegalArgumentException("Metadata source is required");
        }
    }

    private static List<String> subjects(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Map<String, String> unique = new LinkedHashMap<>();
        for (String candidate : values) {
            String subject = text(candidate, MAX_SUBJECT_LENGTH);
            if (subject != null) {
                unique.putIfAbsent(subject.toLowerCase(Locale.ROOT), subject);
            }
            if (unique.size() == MAX_SUBJECTS) {
                break;
            }
        }
        return List.copyOf(unique.values());
    }

    private static String text(String input, int maximumLength) {
        String normalized = value(input, Integer.MAX_VALUE);
        return normalized == null
                ? null
                : truncate(normalized.replaceAll("\\s+", " "), maximumLength);
    }

    private static String value(String input, int maximumLength) {
        if (input == null || input.isBlank()) {
            return null;
        }
        return truncate(input.trim(), maximumLength);
    }

    private static String truncate(String input, int maximumLength) {
        int codePoints = input.codePointCount(0, input.length());
        if (codePoints <= maximumLength) {
            return input;
        }
        return input.substring(0, input.offsetByCodePoints(0, maximumLength));
    }
}
