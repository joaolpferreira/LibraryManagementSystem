package com.example.library.mcp;

import java.util.List;

import org.springframework.data.domain.Page;

public record McpPage<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    static <T> McpPage<T> from(Page<T> source) {
        return new McpPage<>(
                List.copyOf(source.getContent()),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages()
        );
    }
}
