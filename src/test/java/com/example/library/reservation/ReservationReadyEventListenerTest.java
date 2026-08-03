package com.example.library.reservation;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class ReservationReadyEventListenerTest {

    @Test
    void logsReadyReservationEvents() {
        ReservationReadyEvent event = new ReservationReadyEvent(
                1L,
                2L,
                "client",
                Instant.parse("2026-01-01T12:00:00Z"),
                Instant.parse("2026-01-03T12:00:00Z")
        );

        assertThatCode(() -> new ReservationReadyEventListener().onReservationReady(event))
                .doesNotThrowAnyException();
    }
}
