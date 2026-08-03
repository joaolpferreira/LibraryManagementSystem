package com.example.library.mcp;

import com.example.library.book.BookRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpInputValidatorTest {

    private final McpInputValidator inputs = new McpInputValidator(
            Validation.buildDefaultValidatorFactory().getValidator()
    );

    @Test
    void acceptsValidBeanAndReportsInvalidPropertiesInStableOrder() {
        BookRequest valid = new BookRequest(
                "9780132350884",
                "Clean Code",
                "Robert C. Martin",
                null,
                1
        );
        BookRequest invalid = new BookRequest("bad", "", "Author", null, 0);

        inputs.validate(valid);

        assertThatThrownBy(() -> inputs.validate(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Invalid MCP tool input: ")
                .hasMessageContainingAll("isbn", "title", "totalCopies");
    }

    @Test
    void acceptsOnlyPositiveIdentifiers() {
        assertThat(inputs.positiveId(7L, "bookId")).isEqualTo(7L);
        assertThatThrownBy(() -> inputs.positiveId(null, "bookId"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("bookId must be a positive integer");
        assertThatThrownBy(() -> inputs.positiveId(0L, "bookId"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("bookId must be a positive integer");
    }

    @Test
    void appliesPagingDefaultsAndAcceptsExplicitValues() {
        Sort sort = Sort.by("title");

        var defaults = inputs.page(null, null, sort);
        var explicit = inputs.page(2, 50, Sort.unsorted());

        assertThat(defaults.getPageNumber()).isZero();
        assertThat(defaults.getPageSize()).isEqualTo(20);
        assertThat(defaults.getSort()).isEqualTo(sort);
        assertThat(explicit.getPageNumber()).isEqualTo(2);
        assertThat(explicit.getPageSize()).isEqualTo(50);
        assertThat(explicit.getSort().isUnsorted()).isTrue();
    }

    @Test
    void rejectsPagingOutsideSupportedRange() {
        Sort sort = Sort.unsorted();

        assertThatThrownBy(() -> inputs.page(-1, 20, sort))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("page must be zero or greater");
        assertThatThrownBy(() -> inputs.page(0, 0, sort))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("size must be between 1 and 100");
        assertThatThrownBy(() -> inputs.page(0, 101, sort))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("size must be between 1 and 100");
    }

    @Test
    void resolvesAndValidatesBoundedIntegers() {
        assertThat(inputs.integerBetween(null, 5, 1, 20, "limit")).isEqualTo(5);
        assertThat(inputs.integerBetween(20, 5, 1, 20, "limit")).isEqualTo(20);
        assertThatThrownBy(() -> inputs.integerBetween(0, 5, 1, 20, "limit"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit must be between 1 and 20");
        assertThatThrownBy(() -> inputs.integerBetween(21, 5, 1, 20, "limit"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit must be between 1 and 20");
    }
}
