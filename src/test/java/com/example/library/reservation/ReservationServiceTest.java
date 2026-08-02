package com.example.library.reservation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.example.library.book.Book;
import com.example.library.book.BookRepository;
import com.example.library.common.ConflictException;
import com.example.library.common.ResourceNotFoundException;
import com.example.library.config.ReservationProperties;
import com.example.library.loan.LoanRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    @Mock
    private BookReservationRepository reservationRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private LoanRepository loanRepository;
    @Mock
    private LibraryUserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ReservationService service;

    @BeforeEach
    void setUp() {
        service = new ReservationService(
                reservationRepository,
                bookRepository,
                loanRepository,
                userRepository,
                eventPublisher,
                Clock.fixed(NOW, ZoneOffset.UTC),
                new ReservationProperties(Duration.ofHours(48))
        );
        lenient().when(reservationRepository.findReadyForBookForUpdate(anyLong()))
                .thenReturn(List.of());
        lenient().when(reservationRepository.findWaitingForBookForUpdate(anyLong(), any()))
                .thenReturn(List.of());
        lenient().when(reservationRepository.countByBookIdAndStatus(anyLong(), any()))
                .thenReturn(0L);
        lenient().when(reservationRepository.countWaitingAhead(anyLong(), any(), any()))
                .thenReturn(0L);
    }

    @Test
    void unavailableBookCanBeReservedAtTheEndOfTheQueue() {
        Book book = book(1L, 0, 1);
        LibraryUser borrower = borrower("client", 2L);
        ReserveBookRequest request = new ReserveBookRequest(1L);
        when(userRepository.findByUsername("client")).thenReturn(Optional.of(borrower));
        when(bookRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(book));
        when(loanRepository.existsByBookIdAndBorrowerIdAndReturnedAtIsNull(1L, 2L))
                .thenReturn(false);
        when(reservationRepository.existsByActiveKey("1:2")).thenReturn(false);
        when(reservationRepository.save(any(BookReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response = service.reserve(request, "client");

        assertThat(response.status()).isEqualTo(ReservationStatus.WAITING);
        assertThat(response.queuePosition()).isEqualTo(1L);
        assertThat(response.queuedAt()).isEqualTo(NOW);
    }

    @Test
    void reservationRejectsUnknownUserAndMissingBook() {
        ReserveBookRequest request = new ReserveBookRequest(1L);
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reserve(request, "missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Authenticated user");

        LibraryUser borrower = borrower("client", 2L);
        when(userRepository.findByUsername("client")).thenReturn(Optional.of(borrower));
        when(bookRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reserve(request, "client"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Book 1 was not found");
    }

    @Test
    void reservationRejectsActiveLoanDuplicateAndUnallocatedAvailableCopy() {
        ReserveBookRequest request = new ReserveBookRequest(1L);
        LibraryUser borrower = borrower("client", 2L);
        Book unavailable = book(1L, 0, 1);
        when(userRepository.findByUsername("client")).thenReturn(Optional.of(borrower));
        when(bookRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(unavailable));
        when(loanRepository.existsByBookIdAndBorrowerIdAndReturnedAtIsNull(1L, 2L))
                .thenReturn(true);
        assertThatThrownBy(() -> service.reserve(request, "client"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("active loan");

        when(loanRepository.existsByBookIdAndBorrowerIdAndReturnedAtIsNull(1L, 2L))
                .thenReturn(false);
        when(reservationRepository.existsByActiveKey("1:2")).thenReturn(true);
        assertThatThrownBy(() -> service.reserve(request, "client"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("active reservation");

        Book available = book(1L, 1, 1);
        when(bookRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(available));
        when(reservationRepository.existsByActiveKey("1:2")).thenReturn(false);
        assertThatThrownBy(() -> service.reserve(request, "client"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("borrow it instead");
    }

    @Test
    void listAndQueueQueriesMapWaitingPositionsAndTerminalStates() {
        BookReservation waiting = reservation(book(1L, 0, 1), borrower("client", 2L), NOW);
        BookReservation cancelled = reservation(book(1L, 0, 1), borrower("client", 2L), NOW);
        cancelled.cancel(NOW.plusSeconds(1));
        PageRequest pageable = PageRequest.of(0, 20);
        when(reservationRepository.countWaitingAhead(1L, NOW, null)).thenReturn(2L);
        when(reservationRepository.findForBorrower("client", null, pageable))
                .thenReturn(new PageImpl<>(List.of(waiting, cancelled)));
        when(reservationRepository.findAllDetailed(ReservationStatus.CANCELLED, pageable))
                .thenReturn(new PageImpl<>(List.of(cancelled)));
        when(bookRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(waiting.getBook()));
        when(reservationRepository.findActiveQueueForBook(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(waiting)));

        assertThat(service.myReservations("client", null, pageable).getContent())
                .extracting(ReservationResponse::queuePosition)
                .containsExactly(3L, null);
        assertThat(service.allReservations(ReservationStatus.CANCELLED, pageable).getContent())
                .singleElement().extracting(ReservationResponse::status)
                .isEqualTo(ReservationStatus.CANCELLED);
        assertThat(service.queueForBook(1L, pageable).getContent())
                .singleElement().extracting(ReservationResponse::queuePosition).isEqualTo(3L);

        when(bookRepository.findByIdAndActiveTrue(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.queueForBook(2L, pageable))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getEnforcesOwnershipAndAllowsOwners() {
        BookReservation reservation = reservation(book(1L, 0, 1), borrower("client", 2L), NOW);
        when(reservationRepository.findDetailedById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.findDetailedById(2L)).thenReturn(Optional.empty());

        assertThat(service.get(1L, "client", false).borrower().username()).isEqualTo("client");
        assertThat(service.get(1L, "owner", true).borrower().username()).isEqualTo("client");
        assertThatThrownBy(() -> service.get(1L, "other", false))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("only view their own");
        assertThatThrownBy(() -> service.get(2L, "client", false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void clientCanCancelOwnActiveReservation() {
        Book book = book(1L, 0, 1);
        BookReservation reservation = reservation(book, borrower("client", 2L), NOW);
        when(reservationRepository.findDetailedById(1L)).thenReturn(Optional.of(reservation));
        when(bookRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(book));
        when(reservationRepository.findDetailedByIdForUpdate(1L)).thenReturn(Optional.of(reservation));

        ReservationResponse response = service.cancel(1L, "client");

        assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(response.cancelledAt()).isEqualTo(NOW);
    }

    @Test
    void cancellationRejectsMissingForeignInactiveChangedAndTerminalReservations() {
        when(reservationRepository.findDetailedById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cancel(1L, "client"))
                .isInstanceOf(ResourceNotFoundException.class);

        Book book = book(1L, 0, 1);
        BookReservation foreign = reservation(book, borrower("other", 3L), NOW);
        when(reservationRepository.findDetailedById(2L)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.cancel(2L, "client"))
                .isInstanceOf(AccessDeniedException.class);

        BookReservation own = reservation(book, borrower("client", 2L), NOW);
        when(reservationRepository.findDetailedById(3L)).thenReturn(Optional.of(own));
        when(bookRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cancel(3L, "client"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no longer active");

        when(reservationRepository.findDetailedById(4L)).thenReturn(Optional.of(own));
        when(bookRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(book));
        when(reservationRepository.findDetailedByIdForUpdate(4L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cancel(4L, "client"))
                .isInstanceOf(ResourceNotFoundException.class);

        BookReservation fulfilled = reservation(book, borrower("client", 2L), NOW);
        fulfilled.markReady(NOW, NOW.plusSeconds(100));
        fulfilled.fulfill(NOW.plusSeconds(1));
        when(reservationRepository.findDetailedById(5L)).thenReturn(Optional.of(fulfilled));
        when(reservationRepository.findDetailedByIdForUpdate(5L)).thenReturn(Optional.of(fulfilled));
        assertThatThrownBy(() -> service.cancel(5L, "client"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("active reservation");
    }

    @Test
    void claimExpiresOldHoldPromotesNextClientAndReturnsTheirAllocation() {
        Book book = book(1L, 1, 1);
        BookReservation expired = reservation(book, borrower("old", 2L), NOW.minusSeconds(200));
        expired.markReady(NOW.minusSeconds(100), NOW);
        LibraryUser nextBorrower = borrower("next", 3L);
        BookReservation next = reservation(book, nextBorrower, NOW.minusSeconds(50));
        when(reservationRepository.findReadyForBookForUpdate(1L)).thenReturn(List.of(expired));
        when(reservationRepository.findWaitingForBookForUpdate(anyLong(), any()))
                .thenReturn(List.of(next));
        when(reservationRepository.findActiveForBorrowerForUpdate(1L, 3L))
                .thenReturn(Optional.of(next));

        Optional<BookReservation> claim = service.claimForBorrow(book, nextBorrower, NOW);

        assertThat(claim).containsSame(next);
        assertThat(expired.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(next.getStatus()).isEqualTo(ReservationStatus.READY);
        assertThat(next.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(48)));
        ArgumentCaptor<ReservationReadyEvent> event = ArgumentCaptor.forClass(ReservationReadyEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().username()).isEqualTo("next");
    }

    @Test
    void claimProtectsAllocatedCopiesFromOtherClients() {
        Book book = book(1L, 1, 1);
        BookReservation allocated = reservation(book, borrower("first", 2L), NOW.minusSeconds(20));
        allocated.markReady(NOW.minusSeconds(10), NOW.plusSeconds(100));
        LibraryUser waitingBorrower = borrower("waiting", 3L);
        BookReservation waiting = reservation(book, waitingBorrower, NOW);
        when(reservationRepository.findReadyForBookForUpdate(1L)).thenReturn(List.of(allocated));
        when(reservationRepository.findActiveForBorrowerForUpdate(1L, 3L))
                .thenReturn(Optional.of(waiting));
        when(reservationRepository.countByBookIdAndStatus(1L, ReservationStatus.READY))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.claimForBorrow(book, waitingBorrower, NOW))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("reserved for another client");
    }

    @Test
    void claimAllowsUnallocatedCopyAndHandlesZeroCapacity() {
        LibraryUser borrower = borrower("client", 2L);
        Book available = book(1L, 1, 1);
        when(reservationRepository.findActiveForBorrowerForUpdate(1L, 2L))
                .thenReturn(Optional.empty());

        assertThat(service.claimForBorrow(available, borrower, NOW)).isEmpty();

        Book unavailable = book(2L, 0, 1);
        when(reservationRepository.findActiveForBorrowerForUpdate(2L, 2L))
                .thenReturn(Optional.empty());
        assertThat(service.claimForBorrow(unavailable, borrower, NOW)).isEmpty();
    }

    @Test
    void fulfillmentAndReturnReconciliationAdvanceTheQueue() {
        Book book = book(1L, 1, 1);
        BookReservation ready = reservation(book, borrower("client", 2L), NOW.minusSeconds(10));
        ready.markReady(NOW.minusSeconds(5), NOW.plusSeconds(100));
        service.fulfill(ready, NOW);
        assertThat(ready.getStatus()).isEqualTo(ReservationStatus.FULFILLED);

        BookReservation waiting = reservation(book, borrower("next", 3L), NOW);
        when(reservationRepository.findWaitingForBookForUpdate(anyLong(), any()))
                .thenReturn(List.of(waiting));
        service.onBookReturned(book, NOW);
        assertThat(waiting.getStatus()).isEqualTo(ReservationStatus.READY);
    }

    @Test
    void inventoryChangesProtectAllocationsPromoteWaitersAndBlockRemoval() {
        Book book = book(1L, 1, 3);
        BookReservation allocated = reservation(book, borrower("client", 2L), NOW);
        allocated.markReady(NOW.minusSeconds(1), NOW.plusSeconds(100));
        when(reservationRepository.findReadyForBookForUpdate(1L)).thenReturn(List.of(allocated));
        when(reservationRepository.countByBookIdAndStatus(1L, ReservationStatus.READY))
                .thenReturn(1L);

        service.prepareInventoryUpdate(book, 3);
        assertThatThrownBy(() -> service.prepareInventoryUpdate(book, 2))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("allocated to ready reservations");

        BookReservation waiting = reservation(book, borrower("next", 3L), NOW);
        when(reservationRepository.findReadyForBookForUpdate(1L)).thenReturn(List.of());
        when(reservationRepository.findWaitingForBookForUpdate(anyLong(), any()))
                .thenReturn(List.of(waiting));
        service.onInventoryUpdated(book);
        assertThat(waiting.getStatus()).isEqualTo(ReservationStatus.READY);

        Book noCapacity = book(2L, 0, 1);
        when(reservationRepository.existsByActiveKeyStartingWith("2:")).thenReturn(false);
        service.assertNoActiveReservations(noCapacity);
        when(reservationRepository.existsByActiveKeyStartingWith("2:")).thenReturn(true);
        assertThatThrownBy(() -> service.assertNoActiveReservations(noCapacity))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("active reservations");
    }

    @Test
    void scheduledReconciliationLocksExistingBooksAndSkipsRemovedOnes() {
        Book book = book(1L, 0, 1);
        when(reservationRepository.findBookIdsWithExpiredReadyReservations(NOW))
                .thenReturn(List.of(1L, 2L));
        when(bookRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(book));
        when(bookRepository.findActiveByIdForUpdate(2L)).thenReturn(Optional.empty());

        service.reconcileExpiredReservations();

        verify(reservationRepository).findReadyForBookForUpdate(1L);
        verify(reservationRepository, never()).findReadyForBookForUpdate(2L);
    }

    private static BookReservation reservation(
            Book book,
            LibraryUser borrower,
            Instant queuedAt
    ) {
        return new BookReservation(book, borrower, queuedAt);
    }

    private static Book book(Long id, int availableCopies, int totalCopies) {
        Book book = org.mockito.Mockito.mock(Book.class);
        lenient().when(book.getId()).thenReturn(id);
        lenient().when(book.getIsbn()).thenReturn("9780132350884");
        lenient().when(book.getTitle()).thenReturn("Clean Code");
        lenient().when(book.getAvailableCopies()).thenReturn(availableCopies);
        lenient().when(book.getTotalCopies()).thenReturn(totalCopies);
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
