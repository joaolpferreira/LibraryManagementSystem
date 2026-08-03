package com.example.library.config;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationPropertiesTest {

    @Test
    void acceptsPositiveHoldAndLeavesNullForBeanValidation() {
        ReservationProperties properties = new ReservationProperties(Duration.ofHours(48));

        assertThat(properties.readyHold()).isEqualTo(Duration.ofHours(48));
        assertThatCode(() -> new ReservationProperties(null)).doesNotThrowAnyException();
    }

    @Test
    void rejectsNonPositiveReadyHold() {
        assertThatThrownBy(() -> new ReservationProperties(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");
    }
}
