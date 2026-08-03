package com.example.library.mcp;

import java.util.List;

import com.example.library.book.BookRequest;
import com.example.library.book.BookResponse;
import com.example.library.book.BookService;
import com.example.library.fee.LateFeeResponse;
import com.example.library.fee.LateFeeService;
import com.example.library.fee.LateFeeSettlementAction;
import com.example.library.fee.LateFeeSettlementRequest;
import com.example.library.fee.LateFeeStatus;
import com.example.library.loan.BorrowBookRequest;
import com.example.library.loan.LoanResponse;
import com.example.library.loan.LoanService;
import com.example.library.metadata.BookMetadataService;
import com.example.library.metadata.BookMetadataResponse;
import com.example.library.recommendation.RecommendationResponse;
import com.example.library.recommendation.RecommendationService;
import com.example.library.reservation.ReservationResponse;
import com.example.library.reservation.ReservationService;
import com.example.library.reservation.ReservationStatus;
import com.example.library.reservation.ReserveBookRequest;
import com.example.library.search.NaturalLanguageSearchService;
import com.example.library.search.NaturalLanguageSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryMcpToolsTest {

    @Mock
    private BookService bookService;

    @Mock
    private LoanService loanService;

    @Mock
    private ReservationService reservationService;

    @Mock
    private LateFeeService lateFeeService;

    @Mock
    private NaturalLanguageSearchService naturalSearchService;

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private BookMetadataService metadataService;

    @Mock
    private McpAuthentication authentication;

    @Mock
    private McpInputValidator inputs;

    private LibraryMcpTools tools;
    private PageRequest pageable;

    @BeforeEach
    void setUp() {
        tools = new LibraryMcpTools(
                bookService,
                loanService,
                reservationService,
                lateFeeService,
                naturalSearchService,
                recommendationService,
                metadataService,
                authentication,
                inputs
        );
        pageable = PageRequest.of(0, 20);
    }

    @Test
    void delegatesBookAndClientReadTools() {
        BookResponse book = org.mockito.Mockito.mock(BookResponse.class);
        LoanResponse loan = org.mockito.Mockito.mock(LoanResponse.class);
        ReservationResponse reservation = org.mockito.Mockito.mock(ReservationResponse.class);
        LateFeeResponse fee = org.mockito.Mockito.mock(LateFeeResponse.class);
        when(authentication.username()).thenReturn("client");
        when(inputs.page(0, 20, Sort.by("title").ascending())).thenReturn(pageable);
        when(inputs.page(0, 20, Sort.unsorted())).thenReturn(pageable);
        when(inputs.positiveId(1L, "bookId")).thenReturn(1L);
        when(bookService.search("clean", true, pageable))
                .thenReturn(new PageImpl<>(List.of(book), pageable, 1));
        when(bookService.get(1L)).thenReturn(book);
        when(loanService.myHistory("client", pageable))
                .thenReturn(new PageImpl<>(List.of(loan), pageable, 1));
        when(reservationService.myReservations("client", ReservationStatus.WAITING, pageable))
                .thenReturn(new PageImpl<>(List.of(reservation), pageable, 1));
        when(lateFeeService.myFees("client", LateFeeStatus.OUTSTANDING, pageable))
                .thenReturn(new PageImpl<>(List.of(fee), pageable, 1));

        assertThat(tools.searchBooks("clean", true, 0, 20).content()).containsExactly(book);
        assertThat(tools.getBook(1L)).isSameAs(book);
        assertThat(tools.myLoans(0, 20).content()).containsExactly(loan);
        assertThat(tools.myReservations(ReservationStatus.WAITING, 0, 20).content())
                .containsExactly(reservation);
        assertThat(tools.myLateFees(LateFeeStatus.OUTSTANDING, 0, 20).content())
                .containsExactly(fee);
    }

    @Test
    void delegatesClientCommandToolsUsingAuthenticatedIdentity() {
        LoanResponse loan = org.mockito.Mockito.mock(LoanResponse.class);
        ReservationResponse reservation = org.mockito.Mockito.mock(ReservationResponse.class);
        BorrowBookRequest borrowRequest = new BorrowBookRequest(2L, 14);
        ReserveBookRequest reserveRequest = new ReserveBookRequest(2L);
        when(authentication.username()).thenReturn("client");
        when(inputs.positiveId(8L, "loanId")).thenReturn(8L);
        when(inputs.positiveId(9L, "reservationId")).thenReturn(9L);
        when(loanService.borrow(borrowRequest, "client")).thenReturn(loan);
        when(loanService.returnBook(8L, "client")).thenReturn(loan);
        when(reservationService.reserve(reserveRequest, "client")).thenReturn(reservation);
        when(reservationService.cancel(9L, "client")).thenReturn(reservation);

        assertThat(tools.borrowBook(2L, 14)).isSameAs(loan);
        assertThat(tools.returnBook(8L)).isSameAs(loan);
        assertThat(tools.reserveBook(2L)).isSameAs(reservation);
        assertThat(tools.cancelReservation(9L)).isSameAs(reservation);
        verify(inputs).validate(borrowRequest);
        verify(inputs).validate(reserveRequest);
    }

    @Test
    void delegatesOwnerReadTools() {
        LoanResponse loan = org.mockito.Mockito.mock(LoanResponse.class);
        ReservationResponse reservation = org.mockito.Mockito.mock(ReservationResponse.class);
        LateFeeResponse fee = org.mockito.Mockito.mock(LateFeeResponse.class);
        when(inputs.page(1, 10, Sort.unsorted())).thenReturn(pageable);
        when(inputs.positiveId(2L, "bookId")).thenReturn(2L);
        when(loanService.allHistory(pageable))
                .thenReturn(new PageImpl<>(List.of(loan), pageable, 1));
        when(reservationService.allReservations(ReservationStatus.READY, pageable))
                .thenReturn(new PageImpl<>(List.of(reservation), pageable, 1));
        when(reservationService.queueForBook(2L, pageable))
                .thenReturn(new PageImpl<>(List.of(reservation), pageable, 1));
        when(lateFeeService.allFees(LateFeeStatus.PAID, pageable))
                .thenReturn(new PageImpl<>(List.of(fee), pageable, 1));

        assertThat(tools.loanHistory(1, 10).content()).containsExactly(loan);
        assertThat(tools.reservationHistory(ReservationStatus.READY, 1, 10).content())
                .containsExactly(reservation);
        assertThat(tools.bookQueue(2L, 1, 10).content()).containsExactly(reservation);
        assertThat(tools.lateFeeHistory(LateFeeStatus.PAID, 1, 10).content())
                .containsExactly(fee);
    }

    @Test
    void delegatesOwnerInventoryCommandsAndCoversMissingCopyCountValidation() {
        BookResponse book = org.mockito.Mockito.mock(BookResponse.class);
        when(inputs.positiveId(4L, "bookId")).thenReturn(4L);
        when(bookService.create(any(BookRequest.class))).thenReturn(book);
        when(bookService.update(any(Long.class), any(BookRequest.class))).thenReturn(book);

        assertThat(tools.addBook(
                "9780132350884", "Clean Code", "Robert C. Martin", null, 2
        )).isSameAs(book);
        assertThat(tools.addBook(
                "9780132350884", "Clean Code", "Robert C. Martin", null, null
        )).isSameAs(book);
        assertThat(tools.updateBook(
                4L, "9780132350884", "Clean Code", "Robert C. Martin", null, 3
        )).isSameAs(book);
        assertThat(tools.updateBook(
                4L, "9780132350884", "Clean Code", "Robert C. Martin", null, null
        )).isSameAs(book);
        assertThat(tools.removeBook(4L))
                .isEqualTo("Book 4 was removed from the active inventory");

        verify(bookService).remove(4L);
        verify(inputs, org.mockito.Mockito.times(4)).validate(any(BookRequest.class));
    }

    @Test
    void delegatesLateFeeSettlement() {
        LateFeeResponse fee = org.mockito.Mockito.mock(LateFeeResponse.class);
        LateFeeSettlementRequest request = new LateFeeSettlementRequest(
                LateFeeSettlementAction.PAID,
                "Receipt 42"
        );
        when(inputs.positiveId(5L, "lateFeeId")).thenReturn(5L);
        when(lateFeeService.settle(5L, request)).thenReturn(fee);

        assertThat(tools.settleLateFee(5L, LateFeeSettlementAction.PAID, "Receipt 42"))
                .isSameAs(fee);
        verify(inputs).validate(request);
    }

    @Test
    void delegatesNaturalSearchRecommendationsAndMetadataTools() {
        NaturalLanguageSearchResponse search = org.mockito.Mockito.mock(
                NaturalLanguageSearchResponse.class
        );
        RecommendationResponse recommendation = org.mockito.Mockito.mock(
                RecommendationResponse.class
        );
        BookMetadataResponse metadata = org.mockito.Mockito.mock(BookMetadataResponse.class);
        when(inputs.page(0, 10, Sort.by("title").ascending())).thenReturn(pageable);
        when(naturalSearchService.search("available clean code", pageable)).thenReturn(search);
        when(inputs.integerBetween(null, 5, 1, 20, "limit")).thenReturn(5);
        when(authentication.username()).thenReturn("client");
        when(recommendationService.recommend("client", 5))
                .thenReturn(List.of(recommendation));
        when(inputs.positiveId(3L, "bookId")).thenReturn(3L);
        when(metadataService.get(3L)).thenReturn(metadata);
        when(metadataService.enrich(3L)).thenReturn(metadata);

        assertThat(tools.naturalLanguageSearch(" available clean code ", 0, 10))
                .isSameAs(search);
        assertThat(tools.recommendations(null).items()).containsExactly(recommendation);
        assertThat(tools.getBookMetadata(3L)).isSameAs(metadata);
        assertThat(tools.enrichBookMetadata(3L)).isSameAs(metadata);
    }

    @Test
    void rejectsMissingBlankAndOversizedNaturalLanguageQuestions() {
        String oversizedQuestion = "x".repeat(301);

        assertThatThrownBy(() -> tools.naturalLanguageSearch(null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tools.naturalLanguageSearch(" ", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tools.naturalLanguageSearch(oversizedQuestion, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
