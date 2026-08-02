package com.example.library.fee;

import java.math.BigDecimal;
import java.time.Instant;

public record LateFeeRegisteredEvent(
        Long lateFeeId,
        Long loanId,
        String username,
        int daysLate,
        BigDecimal amount,
        String currency,
        Instant registeredAt
) {
}
