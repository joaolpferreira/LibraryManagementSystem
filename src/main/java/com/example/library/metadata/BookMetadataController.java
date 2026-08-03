package com.example.library.metadata;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books/{bookId}/metadata")
public class BookMetadataController {

    private final BookMetadataService metadataService;

    public BookMetadataController(BookMetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'OWNER')")
    public BookMetadataResponse get(@PathVariable Long bookId) {
        return metadataService.get(bookId);
    }

    @PostMapping("/enrich")
    @PreAuthorize("hasRole('OWNER')")
    public BookMetadataResponse enrich(@PathVariable Long bookId) {
        return metadataService.enrich(bookId);
    }
}
