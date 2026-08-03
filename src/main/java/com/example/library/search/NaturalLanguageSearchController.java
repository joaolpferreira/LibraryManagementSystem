package com.example.library.search;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/search")
public class NaturalLanguageSearchController {

    private final NaturalLanguageSearchService searchService;

    public NaturalLanguageSearchController(NaturalLanguageSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/books/natural")
    @PreAuthorize("hasAnyRole('CLIENT', 'OWNER')")
    public NaturalLanguageSearchResponse search(
            @RequestParam @NotBlank @Size(max = 300) String question,
            @PageableDefault(size = 20, sort = "title") Pageable pageable
    ) {
        return searchService.search(question, pageable);
    }
}
