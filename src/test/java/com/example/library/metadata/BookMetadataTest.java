package com.example.library.metadata;

import java.time.Instant;
import java.util.List;

import com.example.library.book.Book;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class BookMetadataTest {

    @Test
    void updatesAndExposesAnImmutableMetadataSnapshot() {
        BookMetadata empty = new BookMetadata();
        assertThat(empty.getBookId()).isNull();

        Book book = new Book("9780132350884", "Clean Code", "Robert C. Martin", null, 1);
        ReflectionTestUtils.setField(book, "id", 7L);
        BookMetadata metadata = new BookMetadata(book);
        ReflectionTestUtils.setField(metadata, "bookId", 7L);
        Instant enrichedAt = Instant.parse("2026-08-01T12:00:00Z");
        metadata.update(new MetadataSnapshot(
                "Prentice Hall",
                2008,
                List.of("Programming", "Software design"),
                "https://covers.test/7.jpg",
                "OPEN_LIBRARY",
                "https://catalog.test/books/7"
        ), enrichedAt);

        BookMetadataResponse response = BookMetadataResponse.from(metadata);

        assertThat(metadata.getBook()).isSameAs(book);
        assertThat(metadata.getPublisher()).isEqualTo("Prentice Hall");
        assertThat(metadata.getPublishedYear()).isEqualTo(2008);
        assertThat(metadata.getSubjects()).containsExactlyInAnyOrder("Programming", "Software design");
        assertThat(metadata.getCoverUrl()).endsWith("7.jpg");
        assertThat(metadata.getSource()).isEqualTo("OPEN_LIBRARY");
        assertThat(metadata.getSourceUrl()).contains("/books/7");
        assertThat(metadata.getEnrichedAt()).isEqualTo(enrichedAt);
        assertThat(response.bookId()).isEqualTo(7L);
        assertThat(response.isbn()).isEqualTo("9780132350884");
        assertThat(response.title()).isEqualTo("Clean Code");
        assertThat(response.subjects()).containsExactly("Programming", "Software design");
        assertThatThrownByModification(metadata);
    }

    private static void assertThatThrownByModification(BookMetadata metadata) {
        assertThat(metadata.getSubjects()).isUnmodifiable();
    }
}
