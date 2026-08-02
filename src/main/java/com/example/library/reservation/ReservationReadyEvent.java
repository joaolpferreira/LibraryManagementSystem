package com.example.library.reservation;

import java.time.Instant;

public record ReservationReadyEvent(
        Long reservationId,
        Long bookId,
        String username,
        Instant readyAt,
        Instant expiresAt
) {
}
