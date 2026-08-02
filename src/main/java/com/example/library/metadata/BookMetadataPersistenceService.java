package com.example.library.metadata;

import java.time.Clock;

import com.example.library.book.Book;
import com.example.library.book.BookRepository;
import com.example.library.common.ConflictException;
import com.example.library.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookMetadataPersistenceService {

    private final BookRepository bookRepository;
    private final BookMetadataRepository metadataRepository;
    private final Clock clock;

    public BookMetadataPersistenceService(
            BookRepository bookRepository,
            BookMetadataRepository metadataRepository,
            Clock clock
    ) {
        this.bookRepository = bookRepository;
        this.metadataRepository = metadataRepository;
        this.clock = clock;
    }

    @Transactional
    public BookMetadataResponse store(
            Long bookId,
            String expectedIsbn,
            MetadataSnapshot snapshot
    ) {
        Book book = bookRepository.findActiveByIdForUpdate(bookId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Book " + bookId + " was not found"
                ));
        if (!book.getIsbn().equals(expectedIsbn)) {
            throw new ConflictException(
                    "The book ISBN changed during metadata lookup; retry enrichment"
            );
        }
        BookMetadata metadata = metadataRepository.findById(bookId)
                .orElseGet(() -> new BookMetadata(book));
        metadata.update(snapshot, clock.instant());
        return BookMetadataResponse.from(metadataRepository.save(metadata));
    }
}
