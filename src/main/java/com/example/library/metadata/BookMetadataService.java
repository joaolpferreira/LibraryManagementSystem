package com.example.library.metadata;

import com.example.library.book.Book;
import com.example.library.book.BookRepository;
import com.example.library.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookMetadataService {

    private final BookRepository bookRepository;
    private final BookMetadataRepository metadataRepository;
    private final BookMetadataProvider metadataProvider;
    private final BookMetadataPersistenceService persistenceService;

    public BookMetadataService(
            BookRepository bookRepository,
            BookMetadataRepository metadataRepository,
            BookMetadataProvider metadataProvider,
            BookMetadataPersistenceService persistenceService
    ) {
        this.bookRepository = bookRepository;
        this.metadataRepository = metadataRepository;
        this.metadataProvider = metadataProvider;
        this.persistenceService = persistenceService;
    }

    @Transactional(readOnly = true)
    public BookMetadataResponse get(Long bookId) {
        assertActiveBook(bookId);
        return metadataRepository.findById(bookId)
                .map(BookMetadataResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Metadata for book " + bookId + " was not found"
                ));
    }

    public BookMetadataResponse enrich(Long bookId) {
        Book book = assertActiveBook(bookId);
        String isbn = book.getIsbn();
        MetadataSnapshot snapshot = metadataProvider.findByIsbn(isbn)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Open Library has no metadata for ISBN " + isbn
                ));
        return persistenceService.store(bookId, isbn, snapshot);
    }

    private Book assertActiveBook(Long bookId) {
        return bookRepository.findByIdAndActiveTrue(bookId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Book " + bookId + " was not found"
                ));
    }
}
