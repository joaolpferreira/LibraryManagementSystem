package com.example.library.search;

import java.util.List;

import com.example.library.book.BookResponse;
import com.example.library.book.BookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NaturalLanguageSearchServiceTest {

    @Mock
    private BookService bookService;

    @Test
    void returnsTheInterpretationResultsAndStablePageMetadata() {
        NaturalLanguageQueryParser parser = new NaturalLanguageQueryParser();
        NaturalLanguageSearchService service = new NaturalLanguageSearchService(parser, bookService);
        PageRequest pageable = PageRequest.of(1, 2);
        BookResponse book = new BookResponse(
                1L, "9780132350884", "Clean Code", "Robert C. Martin", null, 2, 1, true
        );
        when(bookService.search("clean code", true, pageable))
                .thenReturn(new PageImpl<>(List.of(book), pageable, 5));

        NaturalLanguageSearchResponse response = service.search(
                "Find available books: Clean Code",
                pageable
        );

        assertThat(response.interpretation().catalogQuery()).isEqualTo("clean code");
        assertThat(response.books()).containsExactly(book);
        assertThat(response.page()).isEqualTo(new PageSummary(1, 2, 5, 3));
    }
}
