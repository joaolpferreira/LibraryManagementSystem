package com.example.library.fee;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.library.book.Book;
import com.example.library.loan.Loan;
import com.example.library.user.LibraryUser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LateFeeTest {

    private static final Instant REGISTERED_AT = Instant.parse("2026-01-03T12:00:00Z");
    private static final LateFeePolicy.Calculation CALCULATION = new LateFeePolicy.Calculation(
            2,
            new BigDecimal("0.50"),
            new BigDecimal("1.00"),
            "EUR"
    );

    @Test
    void constructorsAndGettersExposeTheImmutableRegistrationSnapshot() {
        LateFee emptyFee = new LateFee();
        assertThat(emptyFee.getId()).isNull();

        Loan loan = loan();
        LateFee fee = new LateFee(loan, CALCULATION, REGISTERED_AT);

        assertThat(fee.getId()).isNull();
        assertThat(fee.getLoan()).isSameAs(loan);
        assertThat(fee.getDaysLate()).isEqualTo(2);
        assertThat(fee.getDailyRate()).isEqualByComparingTo("0.50");
        assertThat(fee.getAmount()).isEqualByComparingTo("1.00");
        assertThat(fee.getCurrency()).isEqualTo("EUR");
        assertThat(fee.getStatus()).isEqualTo(LateFeeStatus.OUTSTANDING);
        assertThat(fee.getRegisteredAt()).isEqualTo(REGISTERED_AT);
        assertThat(fee.getSettledAt()).isNull();
        assertThat(fee.getSettlementNote()).isNull();
    }

    @Test
    void settlementSupportsPaidWaivedAndNormalizedNotes() {
        LateFee paid = new LateFee(loan(), CALCULATION, REGISTERED_AT);
        paid.settle(LateFeeSettlementAction.PAID, REGISTERED_AT.plusSeconds(1), null);
        assertThat(paid.getStatus()).isEqualTo(LateFeeStatus.PAID);
        assertThat(paid.getSettlementNote()).isNull();

        LateFee waivedWithoutNote = new LateFee(loan(), CALCULATION, REGISTERED_AT);
        waivedWithoutNote.settle(LateFeeSettlementAction.WAIVED, REGISTERED_AT.plusSeconds(2), "   ");
        assertThat(waivedWithoutNote.getStatus()).isEqualTo(LateFeeStatus.WAIVED);
        assertThat(waivedWithoutNote.getSettlementNote()).isNull();

        LateFee waivedWithNote = new LateFee(loan(), CALCULATION, REGISTERED_AT);
        waivedWithNote.settle(
                LateFeeSettlementAction.WAIVED,
                REGISTERED_AT.plusSeconds(3),
                "  Courtesy waiver  "
        );
        assertThat(waivedWithNote.getSettlementNote()).isEqualTo("Courtesy waiver");
        assertThat(waivedWithNote.getSettledAt()).isEqualTo(REGISTERED_AT.plusSeconds(3));
    }

    @Test
    void anAlreadySettledFeeCannotBeSettledAgain() {
        LateFee fee = new LateFee(loan(), CALCULATION, REGISTERED_AT);
        fee.settle(LateFeeSettlementAction.PAID, REGISTERED_AT.plusSeconds(1), null);
        Instant duplicateSettlementTime = REGISTERED_AT.plusSeconds(2);

        assertThatThrownBy(() -> fee.settle(
                LateFeeSettlementAction.WAIVED,
                duplicateSettlementTime,
                "Duplicate"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been settled");
    }

    @Test
    void responseIncludesLoanBookAndBorrowerDetails() {
        LateFeeResponse response = LateFeeResponse.from(
                new LateFee(loan(), CALCULATION, REGISTERED_AT)
        );

        assertThat(response.loanId()).isEqualTo(10L);
        assertThat(response.book().title()).isEqualTo("Clean Code");
        assertThat(response.borrower().username()).isEqualTo("client");
        assertThat(response.amount()).isEqualByComparingTo("1.00");
    }

    private static Loan loan() {
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(1L);
        when(book.getIsbn()).thenReturn("9780132350884");
        when(book.getTitle()).thenReturn("Clean Code");
        LibraryUser borrower = mock(LibraryUser.class);
        when(borrower.getUsername()).thenReturn("client");
        when(borrower.getDisplayName()).thenReturn("Demo Client");
        Loan loan = mock(Loan.class);
        when(loan.getId()).thenReturn(10L);
        when(loan.getBook()).thenReturn(book);
        when(loan.getBorrower()).thenReturn(borrower);
        return loan;
    }
}
