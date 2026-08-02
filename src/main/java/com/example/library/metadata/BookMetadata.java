package com.example.library.metadata;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import com.example.library.book.Book;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "book_metadata")
public class BookMetadata {

    @Id
    @Column(name = "book_id")
    private Long bookId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(length = 200)
    private String publisher;

    @Column(name = "published_year")
    private Integer publishedYear;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "book_metadata_subjects",
            joinColumns = @JoinColumn(name = "book_id")
    )
    @Column(name = "subject", nullable = false, length = 200)
    private Set<String> subjects = new LinkedHashSet<>();

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "enriched_at", nullable = false)
    private Instant enrichedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected BookMetadata() {
    }

    public BookMetadata(Book book) {
        this.book = book;
    }

    public void update(MetadataSnapshot snapshot, Instant timestamp) {
        publisher = snapshot.publisher();
        publishedYear = snapshot.publishedYear();
        subjects.clear();
        subjects.addAll(snapshot.subjects());
        coverUrl = snapshot.coverUrl();
        source = snapshot.source();
        sourceUrl = snapshot.sourceUrl();
        enrichedAt = timestamp;
    }

    public Long getBookId() {
        return bookId;
    }

    public Book getBook() {
        return book;
    }

    public String getPublisher() {
        return publisher;
    }

    public Integer getPublishedYear() {
        return publishedYear;
    }

    public Set<String> getSubjects() {
        return Set.copyOf(subjects);
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getSource() {
        return source;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public Instant getEnrichedAt() {
        return enrichedAt;
    }
}
