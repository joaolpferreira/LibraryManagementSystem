CREATE TABLE book_metadata (
    book_id BIGINT PRIMARY KEY,
    publisher VARCHAR(200),
    published_year INTEGER,
    cover_url VARCHAR(500),
    source VARCHAR(50) NOT NULL,
    source_url VARCHAR(500),
    enriched_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_book_metadata_book
        FOREIGN KEY (book_id) REFERENCES books (id),
    CONSTRAINT chk_book_metadata_published_year
        CHECK (published_year IS NULL OR published_year BETWEEN 1000 AND 9999)
);

CREATE TABLE book_metadata_subjects (
    book_id BIGINT NOT NULL,
    subject VARCHAR(200) NOT NULL,
    CONSTRAINT pk_book_metadata_subjects PRIMARY KEY (book_id, subject),
    CONSTRAINT fk_book_metadata_subjects_metadata
        FOREIGN KEY (book_id) REFERENCES book_metadata (book_id) ON DELETE CASCADE
);

CREATE INDEX idx_book_metadata_subject ON book_metadata_subjects (subject);
