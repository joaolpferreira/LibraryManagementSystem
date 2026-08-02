package com.example.library.config;

import java.time.Duration;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "library.reservation")
public record ReservationProperties(@NotNull Duration readyHold) {

    public ReservationProperties {
        if (readyHold != null && !readyHold.isPositive()) {
            throw new IllegalArgumentException("Reservation ready hold must be positive");
        }
    }
}
