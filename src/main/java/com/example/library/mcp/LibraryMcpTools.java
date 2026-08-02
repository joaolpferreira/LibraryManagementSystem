package com.example.library.mcp;

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
import com.example.library.metadata.BookMetadataResponse;
import com.example.library.metadata.BookMetadataService;
import com.example.library.recommendation.RecommendationResponse;
import com.example.library.recommendation.RecommendationService;
import com.example.library.reservation.ReservationResponse;
import com.example.library.reservation.ReservationService;
import com.example.library.reservation.ReservationStatus;
import com.example.library.reservation.ReserveBookRequest;
import com.example.library.search.NaturalLanguageSearchResponse;
import com.example.library.search.NaturalLanguageSearchService;
import java.util.List;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class LibraryMcpTools {

    private static final String CLIENT_ROLE = "hasRole('CLIENT')";
    private static final String OWNER_ROLE = "hasRole('OWNER')";
    private static final String ANY_LIBRARY_ROLE = "hasAnyRole('CLIENT', 'OWNER')";
    private static final String BOOK_ID = "bookId";

    private final BookService bookService;
    private final LoanService loanService;
    private final ReservationService reservationService;
    private final LateFeeService lateFeeService;
    private final NaturalLanguageSearchService naturalSearchService;
    private final RecommendationService recommendationService;
    private final BookMetadataService metadataService;
    private final McpAuthentication authentication;
    private final McpInputValidator inputs;

    public LibraryMcpTools(
            BookService bookService,
            LoanService loanService,
            ReservationService reservationService,
            LateFeeService lateFeeService,
            NaturalLanguageSearchService naturalSearchService,
            RecommendationService recommendationService,
            BookMetadataService metadataService,
            McpAuthentication authentication,
            McpInputValidator inputs
    ) {
        this.bookService = bookService;
        this.loanService = loanService;
        this.reservationService = reservationService;
        this.lateFeeService = lateFeeService;
        this.naturalSearchService = naturalSearchService;
        this.recommendationService = recommendationService;
        this.metadataService = metadataService;
        this.authentication = authentication;
        this.inputs = inputs;
    }

    @McpTool(
            name = "library_search_books",
            description = "Search active library books by title, author, or ISBN and optionally filter by availability.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "Search library books",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            )
    )
    @PreAuthorize(ANY_LIBRARY_ROLE)
    public McpPage<BookResponse> searchBooks(
            @McpToolParam(description = "Optional title, author, ISBN, description, or subject search text", required = false)
            String query,
            @McpToolParam(description = "true for available books, false for unavailable books, omit for all", required = false)
            Boolean availableOnly,
            @McpToolParam(description = "Zero-based result page; defaults to 0", required = false)
            Integer page,
            @McpToolParam(description = "Page size from 1 to 100; defaults to 20", required = false)
            Integer size
    ) {
        var pageable = inputs.page(page, size, Sort.by("title").ascending());
        return McpPage.from(bookService.search(query, availableOnly, pageable));
    }

    @McpTool(
            name = "library_get_book",
            description = "Get the current inventory details for one active book.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "Get a library book",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            )
    )
    @PreAuthorize(ANY_LIBRARY_ROLE)
    public BookResponse getBook(
            @McpToolParam(description = "Positive library book ID", required = true) Long bookId
    ) {
        return bookService.get(inputs.positiveId(bookId, BOOK_ID));
    }

    @McpTool(
            name = "library_natural_language_search",
            description = "Interpret an English or Portuguese catalog question and search by its detected text and availability intent.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "Search books with natural language",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            )
    )
    @PreAuthorize(ANY_LIBRARY_ROLE)
    public NaturalLanguageSearchResponse naturalLanguageSearch(
            @McpToolParam(description = "Natural-language catalog question", required = true)
            String question,
            @McpToolParam(description = "Zero-based result page; defaults to 0", required = false)
            Integer page,
            @McpToolParam(description = "Page size from 1 to 100; defaults to 20", required = false)
            Integer size
    ) {
        if (question == null || question.isBlank() || question.length() > 300) {
            throw new IllegalArgumentException("question must contain between 1 and 300 characters");
        }
        var pageable = inputs.page(page, size, Sort.by("title").ascending());
        return naturalSearchService.search(question.trim(), pageable);
    }

    @McpTool(
            name = "library_get_recommendations",
            description = "Get explainable recommendations for the authenticated client from borrowing history, metadata, popularity, and availability.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "Recommend library books",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            )
    )
    @PreAuthorize(CLIENT_ROLE)
    public List<RecommendationResponse> recommendations(
            @McpToolParam(description = "Number of recommendations from 1 to 20; defaults to 5", required = false)
            Integer limit
    ) {
        int validLimit = inputs.integerBetween(limit, 5, 1, 20, "limit");
        return recommendationService.recommend(authentication.username(), validLimit);
    }

    @McpTool(
            name = "library_get_book_metadata",
            description = "Get the latest persisted external metadata for an active book.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "Get enriched book metadata",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            )
    )
    @PreAuthorize(ANY_LIBRARY_ROLE)
    public BookMetadataResponse getBookMetadata(
            @McpToolParam(description = "Positive library book ID", required = true) Long bookId
    ) {
        return metadataService.get(inputs.positiveId(bookId, BOOK_ID));
    }

    @McpTool(
            name = "library_enrich_book_metadata",
            description = "Fetch trusted Open Library metadata by ISBN and persist it for an active book as the authenticated owner.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "Enrich book metadata",
                    destructiveHint = false,
                    idempotentHint = false,
                    openWorldHint = true
            )
    )
    @PreAuthorize(OWNER_ROLE)
    public BookMetadataResponse enrichBookMetadata(
            @McpToolParam(description = "Positive library book ID", required = true) Long bookId
    ) {
        return metadataService.enrich(inputs.positiveId(bookId, BOOK_ID));
    }

    @McpTool(
            name = "library_borrow_book",
            description = "Borrow an available book for the authenticated client. A READY reservation is claimed automatically.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "Borrow a library book",
                    destructiveHint = false,
                    openWorldHint = false
            )
    )
    @PreAuthorize(CLIENT_ROLE)
    public LoanResponse borrowBook(
            @McpToolParam(description = "Positive library book ID", required = true) Long bookId,
            @McpToolParam(description = "Loan duration from 1 to 60 days", required = true) Integer loanDays
    ) {
        BorrowBookRequest request = new BorrowBookRequest(bookId, loanDays);
        inputs.validate(request);
        return loanService.borrow(request, authentication.username());
    }

    @McpTool(
            name = "library_return_book",
            description = "Return an active loan owned by the authenticated client. This can register a late fee and advance a reservation queue.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "Return a library book",
                    destructiveHint = false,
                    openWorldHint = false
            )
    )
    @PreAuthorize(CLIENT_ROLE)
    public LoanResponse returnBook(
            @McpToolParam(description = "Positive loan ID owned by the authenticated client", required = true)
            Long loanId
    ) {
        return loanService.returnBook(
                inputs.positiveId(loanId, "loanId"),
                authentication.username()
        );
    }

    @McpTool(
            name = "library_get_my_loans",
            description = "List loan history for the authenticated client, newest first.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "List my library loans",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            )
    )
    @PreAuthorize(CLIENT_ROLE)
    public McpPage<LoanResponse> myLoans(
            @McpToolParam(description = "Zero-based result page; defaults to 0", required = false)
            Integer page,
            @McpToolParam(description = "Page size from 1 to 100; defaults to 20", required = false)
            Integer size
    ) {
        var pageable = inputs.page(page, size, Sort.unsorted());
        return McpPage.from(loanService.myHistory(authentication.username(), pageable));
    }

    @McpTool(
            name = "library_reserve_book",
            description = "Join the FIFO queue for a book that has no unallocated available copy.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "Reserve a library book",
                    destructiveHint = false,
                    openWorldHint = false
            )
    )
    @PreAuthorize(CLIENT_ROLE)
    public ReservationResponse reserveBook(
            @McpToolParam(description = "Positive ID of an unavailable book", required = true) Long bookId
    ) {
        ReserveBookRequest request = new ReserveBookRequest(bookId);
        inputs.validate(request);
        return reservationService.reserve(request, authentication.username());
    }

    @McpTool(
            name = "library_get_my_reservations",
            description = "List reservations for the authenticated client, optionally filtered by lifecycle status.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "List my reservations",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            )
    )
    @PreAuthorize(CLIENT_ROLE)
    public McpPage<ReservationResponse> myReservations(
            @McpToolParam(description = "Optional WAITING, READY, FULFILLED, CANCELLED, or EXPIRED filter", required = false)
            ReservationStatus status,
            @McpToolParam(description = "Zero-based result page; defaults to 0", required = false)
            Integer page,
            @McpToolParam(description = "Page size from 1 to 100; defaults to 20", required = false)
            Integer size
    ) {
        var pageable = inputs.page(page, size, Sort.unsorted());
        return McpPage.from(reservationService.myReservations(
                authentication.username(),
                status,
                pageable
        ));
    }

    @McpTool(
            name = "library_cancel_reservation",
            description = "Cancel an active reservation owned by the authenticated client.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "Cancel my reservation",
                    destructiveHint = false,
                    openWorldHint = false
            )
    )
    @PreAuthorize(CLIENT_ROLE)
    public ReservationResponse cancelReservation(
            @McpToolParam(description = "Positive reservation ID owned by the authenticated client", required = true)
            Long reservationId
    ) {
        return reservationService.cancel(
                inputs.positiveId(reservationId, "reservationId"),
                authentication.username()
        );
    }

    @McpTool(
            name = "library_get_my_late_fees",
            description = "List late fees for the authenticated client, optionally filtered by settlement status.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "List my late fees",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            )
    )
    @PreAuthorize(CLIENT_ROLE)
    public McpPage<LateFeeResponse> myLateFees(
            @McpToolParam(description = "Optional OUTSTANDING, PAID, or WAIVED filter", required = false)
            LateFeeStatus status,
            @McpToolParam(description = "Zero-based result page; defaults to 0", required = false)
            Integer page,
            @McpToolParam(description = "Page size from 1 to 100; defaults to 20", required = false)
            Integer size
    ) {
        var pageable = inputs.page(page, size, Sort.unsorted());
        return McpPage.from(lateFeeService.myFees(authentication.username(), status, pageable));
    }

    @McpTool(
            name = "library_add_book",
            description = "Add a new book to the inventory as the authenticated owner.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "Add a library book",
                    destructiveHint = false,
                    openWorldHint = false
            )
    )
    @PreAuthorize(OWNER_ROLE)
    public BookResponse addBook(
            @McpToolParam(description = "Valid ISBN-10 or ISBN-13", required = true) String isbn,
            @McpToolParam(description = "Book title", required = true) String title,
            @McpToolParam(description = "Book author", required = true) String author,
            @McpToolParam(description = "Optional book description", required = false) String description,
            @McpToolParam(description = "Total number of copies from 1 to 10000", required = true)
            Integer totalCopies
    ) {
        BookRequest request = new BookRequest(
                isbn,
                title,
                author,
                description,
                totalCopies == null ? 0 : totalCopies
        );
        inputs.validate(request);
        return bookService.create(request);
    }

    @McpTool(
            name = "library_update_book",
            description = "Update book metadata and total copy count as the authenticated owner.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "Update a library book",
                    destructiveHint = false,
                    openWorldHint = false
            )
    )
    @PreAuthorize(OWNER_ROLE)
    public BookResponse updateBook(
            @McpToolParam(description = "Positive library book ID", required = true) Long bookId,
            @McpToolParam(description = "Valid ISBN-10 or ISBN-13", required = true) String isbn,
            @McpToolParam(description = "Book title", required = true) String title,
            @McpToolParam(description = "Book author", required = true) String author,
            @McpToolParam(description = "Optional book description", required = false) String description,
            @McpToolParam(description = "New total number of copies from 1 to 10000", required = true)
            Integer totalCopies
    ) {
        long validBookId = inputs.positiveId(bookId, BOOK_ID);
        BookRequest request = new BookRequest(
                isbn,
                title,
                author,
                description,
                totalCopies == null ? 0 : totalCopies
        );
        inputs.validate(request);
        return bookService.update(validBookId, request);
    }

    @McpTool(
            name = "library_remove_book",
            description = "Soft-delete a book with no active loans or reservations as the authenticated owner.",
            annotations = @McpTool.McpAnnotations(
                    title = "Remove a library book",
                    destructiveHint = true,
                    openWorldHint = false
            )
    )
    @PreAuthorize(OWNER_ROLE)
    public String removeBook(
            @McpToolParam(description = "Positive library book ID", required = true) Long bookId
    ) {
        long validBookId = inputs.positiveId(bookId, BOOK_ID);
        bookService.remove(validBookId);
        return "Book " + validBookId + " was removed from the active inventory";
    }

    @McpTool(
            name = "library_get_loan_history",
            description = "List complete library loan history for the authenticated owner, newest first.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "List all library loans",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            )
    )
    @PreAuthorize(OWNER_ROLE)
    public McpPage<LoanResponse> loanHistory(
            @McpToolParam(description = "Zero-based result page; defaults to 0", required = false)
            Integer page,
            @McpToolParam(description = "Page size from 1 to 100; defaults to 20", required = false)
            Integer size
    ) {
        var pageable = inputs.page(page, size, Sort.unsorted());
        return McpPage.from(loanService.allHistory(pageable));
    }

    @McpTool(
            name = "library_get_reservation_history",
            description = "List all reservations for the authenticated owner, optionally filtered by lifecycle status.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "List all reservations",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            )
    )
    @PreAuthorize(OWNER_ROLE)
    public McpPage<ReservationResponse> reservationHistory(
            @McpToolParam(description = "Optional WAITING, READY, FULFILLED, CANCELLED, or EXPIRED filter", required = false)
            ReservationStatus status,
            @McpToolParam(description = "Zero-based result page; defaults to 0", required = false)
            Integer page,
            @McpToolParam(description = "Page size from 1 to 100; defaults to 20", required = false)
            Integer size
    ) {
        var pageable = inputs.page(page, size, Sort.unsorted());
        return McpPage.from(reservationService.allReservations(status, pageable));
    }

    @McpTool(
            name = "library_get_book_queue",
            description = "Inspect the active FIFO reservation queue for one book as the authenticated owner.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "Inspect a book queue",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            )
    )
    @PreAuthorize(OWNER_ROLE)
    public McpPage<ReservationResponse> bookQueue(
            @McpToolParam(description = "Positive library book ID", required = true) Long bookId,
            @McpToolParam(description = "Zero-based result page; defaults to 0", required = false)
            Integer page,
            @McpToolParam(description = "Page size from 1 to 100; defaults to 20", required = false)
            Integer size
    ) {
        long validBookId = inputs.positiveId(bookId, BOOK_ID);
        var pageable = inputs.page(page, size, Sort.unsorted());
        return McpPage.from(reservationService.queueForBook(validBookId, pageable));
    }

    @McpTool(
            name = "library_get_late_fee_history",
            description = "List all late fees for the authenticated owner, optionally filtered by settlement status.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "List all late fees",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            )
    )
    @PreAuthorize(OWNER_ROLE)
    public McpPage<LateFeeResponse> lateFeeHistory(
            @McpToolParam(description = "Optional OUTSTANDING, PAID, or WAIVED filter", required = false)
            LateFeeStatus status,
            @McpToolParam(description = "Zero-based result page; defaults to 0", required = false)
            Integer page,
            @McpToolParam(description = "Page size from 1 to 100; defaults to 20", required = false)
            Integer size
    ) {
        var pageable = inputs.page(page, size, Sort.unsorted());
        return McpPage.from(lateFeeService.allFees(status, pageable));
    }

    @McpTool(
            name = "library_settle_late_fee",
            description = "Record an outstanding late fee as paid or waived as the authenticated owner.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "Settle a late fee",
                    destructiveHint = false,
                    openWorldHint = false
            )
    )
    @PreAuthorize(OWNER_ROLE)
    public LateFeeResponse settleLateFee(
            @McpToolParam(description = "Positive late-fee ID", required = true) Long lateFeeId,
            @McpToolParam(description = "Settlement action: PAID or WAIVED", required = true)
            LateFeeSettlementAction action,
            @McpToolParam(description = "Optional settlement note, maximum 500 characters", required = false)
            String note
    ) {
        long validLateFeeId = inputs.positiveId(lateFeeId, "lateFeeId");
        LateFeeSettlementRequest request = new LateFeeSettlementRequest(action, note);
        inputs.validate(request);
        return lateFeeService.settle(validLateFeeId, request);
    }
}
