package com.example.library.reservation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReserveBookRequest(@NotNull @Positive Long bookId) {
}
