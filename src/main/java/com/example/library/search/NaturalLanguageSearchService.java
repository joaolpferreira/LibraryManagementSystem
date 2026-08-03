package com.example.library.search;

import com.example.library.book.BookResponse;
import com.example.library.book.BookService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NaturalLanguageSearchService {

    private final NaturalLanguageQueryParser parser;
    private final BookService bookService;

    public NaturalLanguageSearchService(
            NaturalLanguageQueryParser parser,
            BookService bookService
    ) {
        this.parser = parser;
        this.bookService = bookService;
    }

    @Transactional(readOnly = true)
    public NaturalLanguageSearchResponse search(String question, Pageable pageable) {
        NaturalLanguageQuery interpretation = parser.parse(question);
        Page<BookResponse> results = bookService.search(
                interpretation.catalogQuery(),
                interpretation.availableOnly(),
                pageable
        );
        return NaturalLanguageSearchResponse.from(interpretation, results);
    }
}
