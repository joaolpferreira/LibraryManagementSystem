package com.example.library.metadata;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.example.library.book.Book;
import com.example.library.book.BookRepository;
import com.example.library.common.ConflictException;
import com.example.library.common.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookMetadataPersistenceServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-01T12:00:00Z");

    @Mock
    private BookRepository bookRepository;
    @Mock
    private BookMetadataRepository metadataRepository;

    private BookMetadataPersistenceService service;
    private Book book;
    private MetadataSnapshot snapshot;

    @BeforeEach
    void setUp() {
        service = new BookMetadataPersistenceService(
                bookRepository,
                metadataRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        book = new Book("9780132350884", "Clean Code", "Robert C. Martin", null, 1);
        ReflectionTestUtils.setField(book, "id", 1L);
        snapshot = new MetadataSnapshot(
                "Publisher", 2008, List.of("Programming"), null,
                "OPEN_LIBRARY", "https://openlibrary.org/works/OL1W"
        );
    }

    @Test
    void createsAndRefreshesMetadataUnderTheBookLock() {
        when(bookRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(book));
        when(metadataRepository.save(any(BookMetadata.class))).thenAnswer(invocation -> {
            BookMetadata metadata = invocation.getArgument(0);
            ReflectionTestUtils.setField(metadata, "bookId", 1L);
            return metadata;
        });

        when(metadataRepository.findById(1L)).thenReturn(Optional.empty());
        BookMetadataResponse created = service.store(1L, book.getIsbn(), snapshot);
        assertThat(created.enrichedAt()).isEqualTo(NOW);

        BookMetadata existing = new BookMetadata(book);
        ReflectionTestUtils.setField(existing, "bookId", 1L);
        when(metadataRepository.findById(1L)).thenReturn(Optional.of(existing));
        BookMetadataResponse refreshed = service.store(1L, book.getIsbn(), snapshot);
        assertThat(refreshed.publisher()).isEqualTo("Publisher");
    }

    @Test
    void rejectsMissingBooksAndAnIsbnChangedDuringTheRemoteLookup() {
        when(bookRepository.findActiveByIdForUpdate(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.store(2L, "old", snapshot))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Book 2 was not found");

        when(bookRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(book));
        assertThatThrownBy(() -> service.store(1L, "changed-isbn", snapshot))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ISBN changed");
    }
}
