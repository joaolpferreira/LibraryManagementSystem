package com.example.library.loan;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class BookReturnedEventListenerTest {

    @Test
    void logsEveryReturnedBookEvent() {
        BookReturnedEvent event = new BookReturnedEvent(
                1L,
                2L,
                "client",
                Instant.parse("2026-01-01T12:00:00Z"),
                true
        );

        BookReturnedEventListener listener = new BookReturnedEventListener();

        assertThatCode(() -> listener.onBookReturned(event)).doesNotThrowAnyException();
    }
}
