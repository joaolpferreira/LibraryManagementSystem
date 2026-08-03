package com.example.library.mcp;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class McpPageTest {

    @Test
    void copiesSpringPageMetadataAndContent() {
        var source = new PageImpl<>(List.of("book"), PageRequest.of(2, 5), 14);

        McpPage<String> result = McpPage.from(source);

        assertThat(result.content()).containsExactly("book");
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.totalElements()).isEqualTo(11);
        assertThat(result.totalPages()).isEqualTo(3);
    }
}
