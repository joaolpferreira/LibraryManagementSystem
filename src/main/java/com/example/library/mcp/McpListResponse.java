package com.example.library.mcp;

import java.util.List;

/**
 * Object wrapper for MCP tools that return an ordered, non-paged collection.
 * MCP output schemas must have an object at their root.
 */
public record McpListResponse<T>(List<T> items) {

    public McpListResponse {
        items = List.copyOf(items);
    }
}
