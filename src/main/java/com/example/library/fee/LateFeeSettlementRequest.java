package com.example.library.fee;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LateFeeSettlementRequest(
        @NotNull LateFeeSettlementAction action,
        @Size(max = 500) String note
) {
}
