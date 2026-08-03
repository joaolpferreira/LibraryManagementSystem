package com.example.library.loan;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanService {

    private static final String NOT_FOUND_SUFFIX = " was not found";

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final LibraryUserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final LateFeeService lateFeeService;
    private final ReservationService reservationService;
    private final Clock clock;

    public LoanService(
            BookRepository bookRepository,
            LoanRepository loanRepository,
            LibraryUserRepository userRepository,
            ApplicationEventPublisher eventPublisher,
            LateFeeService lateFeeService,
            ReservationService reservationService,
            Clock clock
    ) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.lateFeeService = lateFeeService;
        this.reservationService = reservationService;
        this.clock = clock;
    }

    @Transactional
    public LoanResponse borrow(BorrowBookRequest request, String username) {
        LibraryUser borrower = findUser(username);
        Book book = bookRepository.findActiveByIdForUpdate(request.bookId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Book " + request.bookId() + NOT_FOUND_SUFFIX
                ));
        if (loanRepository.existsByBookIdAndBorrowerIdAndReturnedAtIsNull(
                book.getId(), borrower.getId())) {
            throw new ConflictException("You already have an active loan for this book");
        }
        Instant borrowedAt = clock.instant();
        Optional<BookReservation> reservation = reservationService.claimForBorrow(
                book,
                borrower,
                borrowedAt
        );
        try {
            book.borrowCopy();
        } catch (IllegalStateException exception) {
            throw new ConflictException(exception.getMessage());
        }
        Instant dueAt = borrowedAt.plus(request.loanDays(), ChronoUnit.DAYS);
        Loan loan = loanRepository.save(new Loan(book, borrower, borrowedAt, dueAt));
        reservation.ifPresent(value -> reservationService.fulfill(value, borrowedAt));
        return LoanResponse.from(loan, borrowedAt);
    }

    @Transactional
    public LoanResponse returnBook(Long loanId, String username) {
        Loan loan = loanRepository.findByIdForUpdate(loanId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Loan " + loanId + NOT_FOUND_SUFFIX
                ));
        if (!loan.getBorrower().getUsername().equals(username)) {
            throw new AccessDeniedException("A client can only return their own loans");
        }
        if (!loan.isActive()) {
            throw new ConflictException("This loan has already been returned");
        }

        Book book = bookRepository.findActiveByIdForUpdate(loan.getBook().getId())
                .orElseThrow(() -> new ConflictException("The borrowed book is no longer active"));
        Instant returnedAt = clock.instant();
        loan.returnBook(returnedAt);
        book.returnCopy();
        lateFeeService.registerIfLate(loan, returnedAt);
        reservationService.onBookReturned(book, returnedAt);

        eventPublisher.publishEvent(new BookReturnedEvent(
                loan.getId(),
                book.getId(),
                username,
                returnedAt,
                loan.isReturnedLate()
        ));
        return LoanResponse.from(loan, returnedAt);
    }

    @Transactional(readOnly = true)
    public Page<LoanResponse> myHistory(String username, Pageable pageable) {
        Instant now = clock.instant();
        return loanRepository.findByBorrowerUsernameOrderByBorrowedAtDesc(username, pageable)
                .map(loan -> LoanResponse.from(loan, now));
    }

    @Transactional(readOnly = true)
    public Page<LoanResponse> allHistory(Pageable pageable) {
        Instant now = clock.instant();
        return loanRepository.findAllByOrderByBorrowedAtDesc(pageable)
                .map(loan -> LoanResponse.from(loan, now));
    }

    @Transactional(readOnly = true)
    public LoanResponse get(Long loanId, String username, boolean owner) {
        Loan loan = loanRepository.findDetailedById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Loan " + loanId + NOT_FOUND_SUFFIX
                ));
        if (!owner && !loan.getBorrower().getUsername().equals(username)) {
            throw new AccessDeniedException("A client can only view their own loans");
        }
        return LoanResponse.from(loan, clock.instant());
    }

    private LibraryUser findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user " + username + " does not exist"
                ));
    }
}
