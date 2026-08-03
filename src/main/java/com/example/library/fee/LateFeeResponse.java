package com.example.library.fee;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.library.loan.Loan;

public record LateFeeResponse(
        Long id,
        Long loanId,
        BookSummary book,
        BorrowerSummary borrower,
        int daysLate,
        BigDecimal dailyRate,
        BigDecimal amount,
        String currency,
        LateFeeStatus status,
        Instant registeredAt,
        Instant settledAt,
        String settlementNote
) {
    public static LateFeeResponse from(LateFee fee) {
        Loan loan = fee.getLoan();
        return new LateFeeResponse(
                fee.getId(),
                loan.getId(),
                new BookSummary(
                        loan.getBook().getId(),
                        loan.getBook().getIsbn(),
                        loan.getBook().getTitle()
                ),
                new BorrowerSummary(
                        loan.getBorrower().getUsername(),
                        loan.getBorrower().getDisplayName()
                ),
                fee.getDaysLate(),
                fee.getDailyRate(),
                fee.getAmount(),
                fee.getCurrency(),
                fee.getStatus(),
                fee.getRegisteredAt(),
                fee.getSettledAt(),
                fee.getSettlementNote()
        );
    }

    public record BookSummary(Long id, String isbn, String title) {
    }

    public record BorrowerSummary(String username, String displayName) {
    }
}
