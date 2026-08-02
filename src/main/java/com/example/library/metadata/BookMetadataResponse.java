package com.example.library.metadata;

import java.time.Instant;
import java.util.List;

public record BookMetadataResponse(
        Long bookId,
        String isbn,
        String title,
        String publisher,
        Integer publishedYear,
        List<String> subjects,
        String coverUrl,
        String source,
        String sourceUrl,
        Instant enrichedAt
) {
    public static BookMetadataResponse from(BookMetadata metadata) {
        return new BookMetadataResponse(
                metadata.getBookId(),
                metadata.getBook().getIsbn(),
                metadata.getBook().getTitle(),
                metadata.getPublisher(),
                metadata.getPublishedYear(),
                metadata.getSubjects().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                metadata.getCoverUrl(),
                metadata.getSource(),
                metadata.getSourceUrl(),
                metadata.getEnrichedAt()
        );
    }
}
