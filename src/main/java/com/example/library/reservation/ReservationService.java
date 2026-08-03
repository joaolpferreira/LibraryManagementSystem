package com.example.library.reservation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    private static final String NOT_FOUND_SUFFIX = " was not found";

    private final BookReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final LibraryUserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final Duration readyHold;

    public ReservationService(
            BookReservationRepository reservationRepository,
            BookRepository bookRepository,
            LoanRepository loanRepository,
            LibraryUserRepository userRepository,
            ApplicationEventPublisher eventPublisher,
            Clock clock,
            ReservationProperties properties
    ) {
        this.reservationRepository = reservationRepository;
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        readyHold = properties.readyHold();
    }

    @Transactional
    public ReservationResponse reserve(ReserveBookRequest request, String username) {
        LibraryUser borrower = findUser(username);
        Book book = bookRepository.findActiveByIdForUpdate(request.bookId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Book " + request.bookId() + NOT_FOUND_SUFFIX
                ));
        Instant now = clock.instant();
        reconcile(book, now);

        if (loanRepository.existsByBookIdAndBorrowerIdAndReturnedAtIsNull(
                book.getId(), borrower.getId())) {
            throw new ConflictException("You already have an active loan for this book");
        }
        if (reservationRepository.existsByActiveKey(
                BookReservation.activeKey(book.getId(), borrower.getId()))) {
            throw new ConflictException("You already have an active reservation for this book");
        }
        long allocatedCopies = reservationRepository.countByBookIdAndStatus(
                book.getId(),
                ReservationStatus.READY
        );
        if (book.getAvailableCopies() > allocatedCopies) {
            throw new ConflictException("This book is available; borrow it instead of joining the queue");
        }

        BookReservation reservation = reservationRepository.save(
                new BookReservation(book, borrower, now)
        );
        return response(reservation);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> myReservations(
            String username,
            ReservationStatus status,
            Pageable pageable
    ) {
        return reservationRepository.findForBorrower(username, status, pageable)
                .map(this::response);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> allReservations(
            ReservationStatus status,
            Pageable pageable
    ) {
        return reservationRepository.findAllDetailed(status, pageable)
                .map(this::response);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> queueForBook(Long bookId, Pageable pageable) {
        if (bookRepository.findByIdAndActiveTrue(bookId).isEmpty()) {
            throw new ResourceNotFoundException("Book " + bookId + NOT_FOUND_SUFFIX);
        }
        return reservationRepository.findActiveQueueForBook(bookId, pageable)
                .map(this::response);
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(Long reservationId, String username, boolean owner) {
        BookReservation reservation = findDetailed(reservationId);
        if (!owner && !reservation.getBorrower().getUsername().equals(username)) {
            throw new AccessDeniedException("A client can only view their own reservations");
        }
        return response(reservation);
    }

    @Transactional
    public ReservationResponse cancel(Long reservationId, String username) {
        BookReservation snapshot = findDetailed(reservationId);
        if (!snapshot.getBorrower().getUsername().equals(username)) {
            throw new AccessDeniedException("A client can only cancel their own reservations");
        }
        Book book = bookRepository.findActiveByIdForUpdate(snapshot.getBook().getId())
                .orElseThrow(() -> new ConflictException("The reserved book is no longer active"));
        BookReservation reservation = reservationRepository.findDetailedByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation " + reservationId + NOT_FOUND_SUFFIX
                ));
        Instant now = clock.instant();
        try {
            reservation.cancel(now);
        } catch (IllegalStateException exception) {
            throw new ConflictException(exception.getMessage());
        }
        reconcile(book, now);
        return response(reservation);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<BookReservation> claimForBorrow(
            Book book,
            LibraryUser borrower,
            Instant borrowedAt
    ) {
        reconcile(book, borrowedAt);
        Optional<BookReservation> activeReservation =
                reservationRepository.findActiveForBorrowerForUpdate(
                        book.getId(),
                        borrower.getId()
                );
        if (activeReservation.filter(
                reservation -> reservation.getStatus() == ReservationStatus.READY
        ).isPresent()) {
            return activeReservation;
        }
        long allocatedCopies = reservationRepository.countByBookIdAndStatus(
                book.getId(),
                ReservationStatus.READY
        );
        if (allocatedCopies >= book.getAvailableCopies() && allocatedCopies > 0) {
            throw new ConflictException("Every available copy is reserved for another client");
        }
        return Optional.empty();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void fulfill(BookReservation reservation, Instant fulfillmentTime) {
        reservation.fulfill(fulfillmentTime);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void onBookReturned(Book book, Instant returnedAt) {
        reconcile(book, returnedAt);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void prepareInventoryUpdate(Book book, int newTotalCopies) {
        reconcile(book, clock.instant());
        int borrowedCopies = book.getTotalCopies() - book.getAvailableCopies();
        int futureAvailableCopies = newTotalCopies - borrowedCopies;
        long allocatedCopies = reservationRepository.countByBookIdAndStatus(
                book.getId(),
                ReservationStatus.READY
        );
        if (futureAvailableCopies < allocatedCopies) {
            throw new ConflictException(
                    "Total copies cannot be lower than copies allocated to ready reservations"
            );
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void onInventoryUpdated(Book book) {
        reconcile(book, clock.instant());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void assertNoActiveReservations(Book book) {
        reconcile(book, clock.instant());
        if (reservationRepository.existsByActiveKeyStartingWith(book.getId() + ":")) {
            throw new ConflictException("A book with active reservations cannot be removed");
        }
    }

    @Transactional
    public void reconcileExpiredReservations() {
        Instant now = clock.instant();
        reservationRepository.findBookIdsWithExpiredReadyReservations(now)
                .forEach(bookId -> bookRepository.findActiveByIdForUpdate(bookId)
                        .ifPresent(book -> reconcile(book, now)));
    }

    private void reconcile(Book book, Instant now) {
        List<BookReservation> readyReservations =
                reservationRepository.findReadyForBookForUpdate(book.getId());
        readyReservations.stream()
                .filter(reservation -> reservation.isReadyAndExpiredAt(now))
                .forEach(reservation -> reservation.expire(now));

        long allocatedCopies = readyReservations.stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.READY)
                .count();
        while (allocatedCopies < book.getAvailableCopies()) {
            List<BookReservation> waiting = reservationRepository.findWaitingForBookForUpdate(
                    book.getId(),
                    PageRequest.of(0, 1)
            );
            if (waiting.isEmpty()) {
                break;
            }
            BookReservation next = waiting.getFirst();
            Instant expiresAt = now.plus(readyHold);
            next.markReady(now, expiresAt);
            eventPublisher.publishEvent(new ReservationReadyEvent(
                    next.getId(),
                    book.getId(),
                    next.getBorrower().getUsername(),
                    now,
                    expiresAt
            ));
            allocatedCopies++;
        }
    }

    private BookReservation findDetailed(Long reservationId) {
        return reservationRepository.findDetailedById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation " + reservationId + NOT_FOUND_SUFFIX
                ));
    }

    private LibraryUser findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user " + username + " does not exist"
                ));
    }

    private ReservationResponse response(BookReservation reservation) {
        Long queuePosition = reservation.getStatus() == ReservationStatus.WAITING
                ? reservationRepository.countWaitingAhead(
                        reservation.getBook().getId(),
                        reservation.getQueuedAt(),
                        reservation.getId()
                ) + 1
                : null;
        return ReservationResponse.from(reservation, queuePosition);
    }
}
