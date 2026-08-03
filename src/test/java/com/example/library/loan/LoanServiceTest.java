package com.example.library.loan;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.example.library.book.Book;
import com.example.library.book.BookRepository;
import com.example.library.common.ConflictException;
import com.example.library.common.ResourceNotFoundException;
import com.example.library.fee.LateFeeService;
import com.example.library.reservation.BookReservation;
import com.example.library.reservation.ReservationService;
import com.example.library.user.LibraryUser;
import com.example.library.user.LibraryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    @Mock
    private BookRepository bookRepository;
    @Mock
    private LoanRepository loanRepository;
    @Mock
    private LibraryUserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private LateFeeService lateFeeService;
    @Mock
    private ReservationService reservationService;

    private LoanService service;
    private LibraryUser borrower;
    private Book book;

    @BeforeEach
    void setUp() {
        service = new LoanService(
                bookRepository,
                loanRepository,
                userRepository,
                eventPublisher,
                lateFeeService,
                reservationService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        borrower = borrower("client", 7L);
        book = book(3L, 1);
    }

    @Test
    void borrowCreatesALoanWithTheRequestedDueDate() {
        BookReservation reservation = org.mockito.Mockito.mock(BookReservation.class);
        when(userRepository.findByUsername("client")).thenReturn(Optional.of(borrower));
        when(bookRepository.findActiveByIdForUpdate(3L)).thenReturn(Optional.of(book));
        when(loanRepository.existsByBookIdAndBorrowerIdAndReturnedAtIsNull(3L, 7L)).thenReturn(false);
        when(reservationService.claimForBorrow(book, borrower, NOW))
                .thenReturn(Optional.of(reservation));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanResponse response = service.borrow(new BorrowBookRequest(3L, 14), "client");

        assertThat(response.borrowedAt()).isEqualTo(NOW);
        assertThat(response.dueAt()).isEqualTo(NOW.plusSeconds(14L * 24 * 60 * 60));
        assertThat(response.status()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(book.getAvailableCopies()).isZero();
        verify(reservationService).fulfill(reservation, NOW);
    }

    @Test
    void borrowRejectsUnknownUserMissingBookDuplicateAndUnavailableBook() {
        BorrowBookRequest request = new BorrowBookRequest(3L, 14);
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.borrow(request, "missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Authenticated user");

        when(userRepository.findByUsername("client")).thenReturn(Optional.of(borrower));
        when(bookRepository.findActiveByIdForUpdate(3L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.borrow(request, "client"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Book 3 was not found");

        when(bookRepository.findActiveByIdForUpdate(3L)).thenReturn(Optional.of(book));
        when(loanRepository.existsByBookIdAndBorrowerIdAndReturnedAtIsNull(3L, 7L)).thenReturn(true);
        assertThatThrownBy(() -> service.borrow(request, "client"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already have");

        Book unavailable = org.mockito.Mockito.mock(Book.class);
        when(unavailable.getId()).thenReturn(3L);
        org.mockito.Mockito.doThrow(new IllegalStateException("No copy of this book is available"))
                .when(unavailable).borrowCopy();
        when(bookRepository.findActiveByIdForUpdate(3L)).thenReturn(Optional.of(unavailable));
        when(loanRepository.existsByBookIdAndBorrowerIdAndReturnedAtIsNull(3L, 7L)).thenReturn(false);
        assertThatThrownBy(() -> service.borrow(request, "client"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("No copy");
    }

    @Test
    void returnBookUpdatesInventoryPublishesEventAndMapsResponse() {
        book.borrowCopy();
        Loan loan = new Loan(book, borrower, NOW.minusSeconds(100), NOW.plusSeconds(100));
        when(loanRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(loan));
        when(bookRepository.findActiveByIdForUpdate(3L)).thenReturn(Optional.of(book));

        LoanResponse response = service.returnBook(5L, "client");

        assertThat(response.status()).isEqualTo(LoanStatus.RETURNED_ON_TIME);
        assertThat(book.getAvailableCopies()).isEqualTo(1);
        verify(lateFeeService).registerIfLate(loan, NOW);
        verify(reservationService).onBookReturned(book, NOW);
        ArgumentCaptor<BookReturnedEvent> event = ArgumentCaptor.forClass(BookReturnedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().username()).isEqualTo("client");
        assertThat(event.getValue().returnedAt()).isEqualTo(NOW);
        assertThat(event.getValue().late()).isFalse();
    }

    @Test
    void returnBookRejectsMissingForeignReturnedAndInactiveBookLoans() {
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.returnBook(1L, "client"))
                .isInstanceOf(ResourceNotFoundException.class);

        Loan foreign = new Loan(book, borrower("someone-else", 8L), NOW, NOW.plusSeconds(10));
        when(loanRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.returnBook(2L, "client"))
                .isInstanceOf(AccessDeniedException.class);

        Loan returned = new Loan(book, borrower, NOW.minusSeconds(20), NOW.minusSeconds(10));
        returned.returnBook(NOW);
        when(loanRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(returned));
        assertThatThrownBy(() -> service.returnBook(3L, "client"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already been returned");

        Loan inactiveBookLoan = new Loan(book, borrower, NOW, NOW.plusSeconds(10));
        when(loanRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(inactiveBookLoan));
        when(bookRepository.findActiveByIdForUpdate(3L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.returnBook(4L, "client"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no longer active");

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void historyMethodsMapRepositoryPagesAtTheCurrentTime() {
        Loan loan = new Loan(book, borrower, NOW.minusSeconds(60), NOW.plusSeconds(60));
        PageRequest pageable = PageRequest.of(0, 20);
        when(loanRepository.findByBorrowerUsernameOrderByBorrowedAtDesc("client", pageable))
                .thenReturn(new PageImpl<>(List.of(loan)));
        when(loanRepository.findAllByOrderByBorrowedAtDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(loan)));

        assertThat(service.myHistory("client", pageable).getContent())
                .singleElement().extracting(LoanResponse::status).isEqualTo(LoanStatus.ACTIVE);
        assertThat(service.allHistory(pageable).getContent())
                .singleElement().extracting(LoanResponse::status).isEqualTo(LoanStatus.ACTIVE);
    }

    @Test
    void getEnforcesOwnershipButAllowsOwners() {
        Loan ownLoan = new Loan(book, borrower, NOW, NOW.plusSeconds(10));
        when(loanRepository.findDetailedById(1L)).thenReturn(Optional.of(ownLoan));
        when(loanRepository.findDetailedById(2L)).thenReturn(Optional.empty());

        assertThat(service.get(1L, "client", false).borrower().username()).isEqualTo("client");
        assertThat(service.get(1L, "owner", true).borrower().username()).isEqualTo("client");
        assertThatThrownBy(() -> service.get(1L, "someone-else", false))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.get(2L, "client", false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static Book book(Long id, int copies) {
        Book book = org.mockito.Mockito.mock(Book.class);
        lenient().when(book.getId()).thenReturn(id);
        lenient().when(book.getIsbn()).thenReturn("9780132350884");
        lenient().when(book.getTitle()).thenReturn("Clean Code");
        lenient().when(book.getAvailableCopies()).thenReturn(copies);
        lenient().doAnswer(invocation -> {
            when(book.getAvailableCopies()).thenReturn(copies - 1);
            return null;
        }).when(book).borrowCopy();
        lenient().doAnswer(invocation -> {
            when(book.getAvailableCopies()).thenReturn(copies);
            return null;
        }).when(book).returnCopy();
        return book;
    }

    private static LibraryUser borrower(String username, Long id) {
        LibraryUser borrower = org.mockito.Mockito.mock(LibraryUser.class);
        lenient().when(borrower.getId()).thenReturn(id);
        lenient().when(borrower.getUsername()).thenReturn(username);
        lenient().when(borrower.getDisplayName()).thenReturn("Demo Client");
        return borrower;
    }
}
