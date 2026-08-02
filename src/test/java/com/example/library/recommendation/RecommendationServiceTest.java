package com.example.library.recommendation;

import java.util.Collection;
import java.util.List;

import com.example.library.book.Book;
import com.example.library.book.BookRepository;
import com.example.library.loan.LoanRepository;
import com.example.library.metadata.BookMetadata;
import com.example.library.metadata.BookMetadataRepository;
import com.example.library.metadata.MetadataSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private LoanRepository loanRepository;
    @Mock
    private BookMetadataRepository metadataRepository;

    private RecommendationService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationService(bookRepository, loanRepository, metadataRepository);
    }

    @Test
    void ranksPersonalizedSignalsAndExplainsEveryScore() {
        Book historyBook = book(1L, "History", "Author A", 1, true);
        Book strongest = book(2L, "Strongest", "Author A", 1, true);
        Book discovery = book(3L, "Discovery", "Author B", 1, false);
        Book popular = book(4L, "Popular", "Author C", 1, true);
        when(loanRepository.findBorrowedBooksForRecommendations("client"))
                .thenReturn(List.of(historyBook));
        when(bookRepository.findByActiveTrueOrderByTitleAsc())
                .thenReturn(List.of(historyBook, strongest, discovery, popular));

        BookLoanCount highCount = count(2L, 10L);
        BookLoanCount lowCount = count(4L, 1L);
        when(loanRepository.countLoansByBook()).thenReturn(List.of(highCount, lowCount));
        when(metadataRepository.findByBookIdIn(any())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            if (ids.contains(1L)) {
                return List.of(metadata(historyBook, "One", "Two", "Three", "Four"));
            }
            return List.of(metadata(strongest, "One", "Two", "Three", "Four"));
        });

        List<RecommendationResponse> recommendations = service.recommend("client", 10);

        assertThat(recommendations).extracting(result -> result.book().id())
                .containsExactly(2L, 4L, 3L);
        assertThat(recommendations.getFirst().score()).isEqualTo(21);
        assertThat(recommendations.getFirst().reasons()).containsExactly(
                "Available now",
                "Matches an author from your borrowing history",
                "Shares subjects with books you borrowed",
                "Popular with library clients"
        );
        assertThat(recommendations.get(1).score()).isEqualTo(2);
        assertThat(recommendations.getLast().reasons())
                .containsExactly("Explore something new from the catalog");
        assertThat(recommendations.getFirst().reasons()).isUnmodifiable();
    }

    @Test
    void coldStartUsesAvailabilityAndStableAlphabeticalTieBreaking() {
        Book zulu = book(2L, "Zulu", "Author Z", 1, true);
        Book alpha = book(3L, "Alpha", "Author A", 1, true);
        when(loanRepository.findBorrowedBooksForRecommendations("new-client"))
                .thenReturn(List.of());
        when(bookRepository.findByActiveTrueOrderByTitleAsc()).thenReturn(List.of(zulu, alpha));
        when(loanRepository.countLoansByBook()).thenReturn(List.of());
        when(metadataRepository.findByBookIdIn(List.of(2L, 3L))).thenReturn(List.of());

        List<RecommendationResponse> recommendations = service.recommend("new-client", 1);

        assertThat(recommendations).singleElement()
                .extracting(result -> result.book().title())
                .isEqualTo("Alpha");
    }

    @Test
    void returnsEmptyWhenEveryActiveBookWasAlreadyBorrowed() {
        Book historyBook = book(1L, "Only book", "Author", 1, true);
        when(loanRepository.findBorrowedBooksForRecommendations("client"))
                .thenReturn(List.of(historyBook));
        when(bookRepository.findByActiveTrueOrderByTitleAsc()).thenReturn(List.of(historyBook));
        when(loanRepository.countLoansByBook()).thenReturn(List.of());
        when(metadataRepository.findByBookIdIn(java.util.Set.of(1L))).thenReturn(List.of());

        assertThat(service.recommend("client", 5)).isEmpty();
        verify(metadataRepository, never()).findByBookIdIn(List.of());
    }

    @Test
    void rejectsLimitsOutsideThePublicContract() {
        assertThatThrownBy(() -> service.recommend("client", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Recommendation limit must be between 1 and 20");
        assertThatThrownBy(() -> service.recommend("client", 21))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Recommendation limit must be between 1 and 20");
    }

    private static Book book(Long id, String title, String author, int copies, boolean available) {
        Book book = new Book("isbn-" + id, title, author, null, copies);
        ReflectionTestUtils.setField(book, "id", id);
        if (!available) {
            book.borrowCopy();
        }
        return book;
    }

    private static BookMetadata metadata(Book book, String... subjects) {
        BookMetadata metadata = new BookMetadata(book);
        ReflectionTestUtils.setField(metadata, "bookId", book.getId());
        metadata.update(
                new MetadataSnapshot(null, null, List.of(subjects), null, "TEST", null),
                java.time.Instant.parse("2026-08-01T00:00:00Z")
        );
        return metadata;
    }

    private static BookLoanCount count(long bookId, long loanCount) {
        BookLoanCount count = mock(BookLoanCount.class);
        when(count.getBookId()).thenReturn(bookId);
        when(count.getLoanCount()).thenReturn(loanCount);
        return count;
    }
}
