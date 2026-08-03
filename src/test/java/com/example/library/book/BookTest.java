package com.example.library.book;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookTest {

    @Test
    void persistenceCallbacksAndGettersExposeTheBookState() {
        Book emptyBook = new Book();
        assertThat(emptyBook.getId()).isNull();

        Book book = new Book("9780132350884", "Clean Code", "Robert C. Martin", "A classic", 2);
        book.onCreate();

        assertThat(book.getId()).isNull();
        assertThat(book.getIsbn()).isEqualTo("9780132350884");
        assertThat(book.getTitle()).isEqualTo("Clean Code");
        assertThat(book.getAuthor()).isEqualTo("Robert C. Martin");
        assertThat(book.getDescription()).isEqualTo("A classic");
        assertThat(book.getTotalCopies()).isEqualTo(2);
        assertThat(book.getAvailableCopies()).isEqualTo(2);
        assertThat(book.isActive()).isTrue();

        book.onUpdate();
    }

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

    @Test
    void borrowingRequiresAnActiveBookWithAvailableCopies() {
        Book unavailable = new Book("9780132350884", "Clean Code", "Robert C. Martin", null, 1);
        unavailable.borrowCopy();

        assertThatThrownBy(unavailable::borrowCopy)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No copy");

        Book inactive = new Book("9780321356680", "Effective Java", "Joshua Bloch", null, 1);
        inactive.deactivate();

        assertThatThrownBy(inactive::borrowCopy)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No copy");
    }

    @Test
    void returningACopyRejectsAnAlreadyFullInventory() {
        Book book = new Book("9780132350884", "Clean Code", "Robert C. Martin", null, 1);

        assertThatThrownBy(book::returnCopy)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already in the inventory");

        book.borrowCopy();
        book.returnCopy();
        assertThat(book.getAvailableCopies()).isEqualTo(1);
    }

    @Test
    void bookCanBeDeactivatedWhenEveryCopyIsAvailable() {
        Book book = new Book("9780132350884", "Clean Code", "Robert C. Martin", null, 1);

        book.deactivate();

        assertThat(book.isActive()).isFalse();
    }
}
