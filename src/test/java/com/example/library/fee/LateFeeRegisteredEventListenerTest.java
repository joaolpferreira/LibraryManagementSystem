package com.example.library.fee;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class LateFeeRegisteredEventListenerTest {

    @Test
    void logsRegisteredFeeEvents() {
        LateFeeRegisteredEvent event = new LateFeeRegisteredEvent(
                1L,
                2L,
                "client",
                3,
                new BigDecimal("1.50"),
                "EUR",
                Instant.parse("2026-01-01T12:00:00Z")
        );
        LateFeeRegisteredEventListener listener = new LateFeeRegisteredEventListener();

        assertThatCode(() -> listener.onLateFeeRegistered(event)).doesNotThrowAnyException();
    }
}
