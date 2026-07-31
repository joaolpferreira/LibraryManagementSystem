package com.example.library.loan;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BorrowBookRequest(
        @NotNull @Positive Long bookId,
        @NotNull @Min(1) @Max(60) Integer loanDays
) {
}

