package com.example.library.loan;

import java.time.Instant;

public record LoanResponse(
        Long id,
        BookSummary book,
        BorrowerSummary borrower,
        Instant borrowedAt,
        Instant dueAt,
        Instant returnedAt,
        boolean returnedLate,
        LoanStatus status
) {
    public static LoanResponse from(Loan loan, Instant now) {
        return new LoanResponse(
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
                loan.getBorrowedAt(),
                loan.getDueAt(),
                loan.getReturnedAt(),
                loan.isReturnedLate(),
                statusOf(loan, now)
        );
    }

    private static LoanStatus statusOf(Loan loan, Instant now) {
        if (loan.isActive()) {
            return now.isAfter(loan.getDueAt()) ? LoanStatus.OVERDUE : LoanStatus.ACTIVE;
        }
        return loan.isReturnedLate() ? LoanStatus.RETURNED_LATE : LoanStatus.RETURNED_ON_TIME;
    }

    public record BookSummary(Long id, String isbn, String title) {
    }

    public record BorrowerSummary(String username, String displayName) {
    }
}

