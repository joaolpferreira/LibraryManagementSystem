package com.example.library.loan;

import java.time.Instant;

import com.example.library.book.Book;
import com.example.library.user.LibraryUser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoanTest {

    private static final Instant BORROWED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant DUE_AT = Instant.parse("2026-01-15T00:00:00Z");

    @Test
    void constructorsAndGettersExposeTheLoanState() {
        Loan emptyLoan = new Loan();
        assertThat(emptyLoan.getId()).isNull();

        Book book = mock(Book.class);
        LibraryUser borrower = mock(LibraryUser.class);
        Loan loan = new Loan(book, borrower, BORROWED_AT, DUE_AT);

        assertThat(loan.getId()).isNull();
        assertThat(loan.getBook()).isSameAs(book);
        assertThat(loan.getBorrower()).isSameAs(borrower);
        assertThat(loan.getBorrowedAt()).isEqualTo(BORROWED_AT);
        assertThat(loan.getDueAt()).isEqualTo(DUE_AT);
        assertThat(loan.getReturnedAt()).isNull();
        assertThat(loan.isReturnedLate()).isFalse();
        assertThat(loan.isActive()).isTrue();
    }

    @Test
    void returnTracksOnTimeAndLateOutcomesAndRejectsASecondReturn() {
        Loan onTime = loan();
        onTime.returnBook(DUE_AT);
        assertThat(onTime.isReturnedLate()).isFalse();
        assertThat(onTime.isActive()).isFalse();

        Instant afterDueAt = DUE_AT.plusSeconds(1);
        assertThatThrownBy(() -> onTime.returnBook(afterDueAt))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been returned");

        Loan late = loan();
        late.returnBook(DUE_AT.plusSeconds(1));
        assertThat(late.isReturnedLate()).isTrue();
        assertThat(late.getReturnedAt()).isEqualTo(DUE_AT.plusSeconds(1));
    }

    @Test
    void responseMapsAllFourLoanStatuses() {
        Loan active = loan();
        assertThat(LoanResponse.from(active, DUE_AT).status()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(LoanResponse.from(active, DUE_AT.plusSeconds(1)).status()).isEqualTo(LoanStatus.OVERDUE);

        Loan returnedOnTime = loan();
        returnedOnTime.returnBook(DUE_AT);
        assertThat(LoanResponse.from(returnedOnTime, DUE_AT).status())
                .isEqualTo(LoanStatus.RETURNED_ON_TIME);

        Loan returnedLate = loan();
        returnedLate.returnBook(DUE_AT.plusSeconds(1));
        LoanResponse response = LoanResponse.from(returnedLate, DUE_AT.plusSeconds(1));
        assertThat(response.status()).isEqualTo(LoanStatus.RETURNED_LATE);
        assertThat(response.book().title()).isEqualTo("Clean Code");
        assertThat(response.borrower().displayName()).isEqualTo("Demo Client");
    }

    private static Loan loan() {
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(1L);
        when(book.getIsbn()).thenReturn("9780132350884");
        when(book.getTitle()).thenReturn("Clean Code");
        LibraryUser borrower = mock(LibraryUser.class);
        when(borrower.getUsername()).thenReturn("client");
        when(borrower.getDisplayName()).thenReturn("Demo Client");
        return new Loan(book, borrower, BORROWED_AT, DUE_AT);
    }
}
