package com.example.library.search;

import org.springframework.data.domain.Page;

public record PageSummary(
        int number,
        int size,
        long totalElements,
        int totalPages
) {
    public static PageSummary from(Page<?> page) {
        return new PageSummary(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
