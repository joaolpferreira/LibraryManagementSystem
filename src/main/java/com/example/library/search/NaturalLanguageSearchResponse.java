package com.example.library.search;

import java.util.List;

import com.example.library.book.BookResponse;
import org.springframework.data.domain.Page;

public record NaturalLanguageSearchResponse(
        NaturalLanguageQuery interpretation,
        List<BookResponse> books,
        PageSummary page
) {
    public static NaturalLanguageSearchResponse from(
            NaturalLanguageQuery interpretation,
            Page<BookResponse> results
    ) {
        return new NaturalLanguageSearchResponse(
                interpretation,
                results.getContent(),
                PageSummary.from(results)
        );
    }
}
