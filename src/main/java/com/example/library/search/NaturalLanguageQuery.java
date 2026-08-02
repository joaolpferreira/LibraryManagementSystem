package com.example.library.search;

public record NaturalLanguageQuery(
        String originalQuestion,
        String catalogQuery,
        Boolean availableOnly
) {
}
