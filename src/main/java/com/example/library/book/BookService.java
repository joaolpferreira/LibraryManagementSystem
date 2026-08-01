package com.example.library.book;

import com.example.library.common.ConflictException;
import com.example.library.common.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Transactional(readOnly = true)
    public Page<BookResponse> search(String query, Boolean availableOnly, Pageable pageable) {
        String normalizedQuery = query == null || query.isBlank() ? "" : query.trim();
        int availabilityFilter = availableOnly == null ? -1 : availableOnly ? 1 : 0;
        return bookRepository.search(normalizedQuery, availabilityFilter, pageable)
                .map(BookResponse::from);
    }

    @Transactional(readOnly = true)
    public BookResponse get(Long id) {
        return BookResponse.from(findActive(id));
    }

    @Transactional
    public BookResponse create(BookRequest request) {
        String isbn = normalizeIsbn(request.isbn());
        if (bookRepository.existsByIsbnIgnoreCase(isbn)) {
            throw new ConflictException("A book with ISBN " + isbn + " already exists");
        }
        Book book = new Book(
                isbn,
                request.title().trim(),
                request.author().trim(),
                normalizeDescription(request.description()),
                request.totalCopies()
        );
        return BookResponse.from(bookRepository.save(book));
    }

    @Transactional
    public BookResponse update(Long id, BookRequest request) {
        Book book = bookRepository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book " + id + " was not found"));
        String isbn = normalizeIsbn(request.isbn());
        if (bookRepository.existsByIsbnIgnoreCaseAndIdNot(isbn, id)) {
            throw new ConflictException("A book with ISBN " + isbn + " already exists");
        }
        try {
            book.update(
                    isbn,
                    request.title().trim(),
                    request.author().trim(),
                    normalizeDescription(request.description()),
                    request.totalCopies()
            );
        } catch (IllegalArgumentException exception) {
            throw new ConflictException(exception.getMessage());
        }
        return BookResponse.from(book);
    }

    @Transactional
    public void remove(Long id) {
        Book book = bookRepository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book " + id + " was not found"));
        try {
            book.deactivate();
        } catch (IllegalStateException exception) {
            throw new ConflictException(exception.getMessage());
        }
    }

    private Book findActive(Long id) {
        return bookRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book " + id + " was not found"));
    }

    private static String normalizeIsbn(String isbn) {
        return isbn.trim().replace("-", "").replace(" ", "");
    }

    private static String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}
