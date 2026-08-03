package com.example.library.metadata;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.example.library.book.Book;
import com.example.library.book.BookRepository;
import com.example.library.common.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookMetadataServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-01T12:00:00Z");

    @Mock
    private BookRepository bookRepository;
    @Mock
    private BookMetadataRepository metadataRepository;
    @Mock
    private BookMetadataProvider metadataProvider;
    @Mock
    private BookMetadataPersistenceService persistenceService;

    private BookMetadataService service;
    private Book book;
    private MetadataSnapshot snapshot;

    @BeforeEach
    void setUp() {
        service = new BookMetadataService(
                bookRepository,
                metadataRepository,
                metadataProvider,
                persistenceService
        );
        book = new Book("9780132350884", "Clean Code", "Robert C. Martin", null, 1);
        ReflectionTestUtils.setField(book, "id", 1L);
        snapshot = new MetadataSnapshot(
                "Publisher", 2008, List.of("Programming"), null,
                "OPEN_LIBRARY", "https://openlibrary.org/books/OL1M"
        );
    }

    @Test
    void getsPersistedMetadataOrReportsWhichResourceIsMissing() {
        BookMetadata metadata = metadata();
        when(bookRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(book));
        when(metadataRepository.findById(1L)).thenReturn(Optional.of(metadata));

        assertThat(service.get(1L).publisher()).isEqualTo("Publisher");

        when(bookRepository.findByIdAndActiveTrue(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Book 2 was not found");

        when(bookRepository.findByIdAndActiveTrue(3L)).thenReturn(Optional.of(book));
        when(metadataRepository.findById(3L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(3L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Metadata for book 3 was not found");
    }

    @Test
    void fetchesOutsidePersistenceAndHandlesProviderMisses() {
        when(bookRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(book));
        when(metadataProvider.findByIsbn(book.getIsbn())).thenReturn(Optional.of(snapshot));
        BookMetadataResponse stored = BookMetadataResponse.from(metadata());
        when(persistenceService.store(1L, book.getIsbn(), snapshot)).thenReturn(stored);
        assertThat(service.enrich(1L).publisher()).isEqualTo("Publisher");

        when(metadataProvider.findByIsbn(book.getIsbn())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.enrich(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("has no metadata");
    }

    private BookMetadata metadata() {
        BookMetadata metadata = new BookMetadata(book);
        ReflectionTestUtils.setField(metadata, "bookId", 1L);
        metadata.update(snapshot, NOW);
        return metadata;
    }
}
