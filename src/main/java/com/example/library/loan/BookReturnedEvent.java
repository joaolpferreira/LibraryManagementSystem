package com.example.library.loan;

import java.time.Instant;

public record BookReturnedEvent(
        Long loanId,
        Long bookId,
        String username,
        Instant returnedAt,
        boolean late
) {
}

