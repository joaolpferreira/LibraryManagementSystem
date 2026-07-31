package com.example.library.book;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookTest {

    @Test
    void changingCopyCountPreservesBorrowedCopies() {
        Book book = new Book("9780132350884", "Clean Code", "Robert C. Martin", null, 3);
        book.borrowCopy();

        book.update("9780132350884", "Clean Code", "Robert C. Martin", null, 5);

        assertThat(book.getTotalCopies()).isEqualTo(5);
        assertThat(book.getAvailableCopies()).isEqualTo(4);
    }

    @Test
    void cannotReduceInventoryBelowActiveLoanCount() {
        Book book = new Book("9780132350884", "Clean Code", "Robert C. Martin", null, 2);
        book.borrowCopy();
        book.borrowCopy();

        assertThatThrownBy(() ->
                book.update("9780132350884", "Clean Code", "Robert C. Martin", null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currently on loan");
    }

    @Test
    void cannotDeactivateBookWhileCopiesAreBorrowed() {
        Book book = new Book("9780132350884", "Clean Code", "Robert C. Martin", null, 1);
        book.borrowCopy();

        assertThatThrownBy(book::deactivate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active loans");
    }
}

