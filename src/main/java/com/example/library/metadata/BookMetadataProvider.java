package com.example.library.metadata;

import java.util.Optional;

public interface BookMetadataProvider {

    Optional<MetadataSnapshot> findByIsbn(String isbn);
}
