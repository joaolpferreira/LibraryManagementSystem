package com.example.library.reservation;

import java.time.Instant;

public record ReservationResponse(
        Long id,
        BookSummary book,
        BorrowerSummary borrower,
        ReservationStatus status,
        Long queuePosition,
        Instant queuedAt,
        Instant readyAt,
        Instant expiresAt,
        Instant fulfilledAt,
        Instant cancelledAt,
        Instant expiredAt
) {
    public static ReservationResponse from(BookReservation reservation, Long queuePosition) {
        return new ReservationResponse(
                reservation.getId(),
                new BookSummary(
                        reservation.getBook().getId(),
                        reservation.getBook().getIsbn(),
                        reservation.getBook().getTitle()
                ),
                new BorrowerSummary(
                        reservation.getBorrower().getUsername(),
                        reservation.getBorrower().getDisplayName()
                ),
                reservation.getStatus(),
                queuePosition,
                reservation.getQueuedAt(),
                reservation.getReadyAt(),
                reservation.getExpiresAt(),
                reservation.getFulfilledAt(),
                reservation.getCancelledAt(),
                reservation.getExpiredAt()
        );
    }

    public record BookSummary(Long id, String isbn, String title) {
    }

    public record BorrowerSummary(String username, String displayName) {
    }
}
