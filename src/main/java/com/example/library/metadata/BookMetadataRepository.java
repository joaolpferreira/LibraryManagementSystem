package com.example.library.metadata;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookMetadataRepository extends JpaRepository<BookMetadata, Long> {

    @EntityGraph(attributePaths = {"book", "subjects"})
    List<BookMetadata> findByBookIdIn(Collection<Long> bookIds);
}
