package com.example.library.recommendation;

import java.util.List;

import com.example.library.book.BookResponse;

public record RecommendationResponse(
        BookResponse book,
        int score,
        List<String> reasons
) {
    public RecommendationResponse {
        reasons = List.copyOf(reasons);
    }
}
