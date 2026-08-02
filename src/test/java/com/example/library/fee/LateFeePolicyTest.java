package com.example.library.fee;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.library.config.LateFeeProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LateFeePolicyTest {

    private static final Instant DUE_AT = Instant.parse("2026-01-01T12:00:00Z");
    private final LateFeePolicy policy = new LateFeePolicy(
            new LateFeeProperties(new BigDecimal("0.50"), "EUR")
    );

    @Test
    void doesNotChargeReturnsAtOrBeforeTheDueTime() {
        assertThat(policy.calculate(DUE_AT, DUE_AT)).isEmpty();
        assertThat(policy.calculate(DUE_AT, DUE_AT.minusSeconds(1))).isEmpty();
    }

    @Test
    void chargesExactCompleteOverdueDays() {
        LateFeePolicy.Calculation calculation = policy.calculate(
                DUE_AT,
                DUE_AT.plusSeconds(2L * 24 * 60 * 60)
        ).orElseThrow();

        assertThat(calculation.daysLate()).isEqualTo(2);
        assertThat(calculation.dailyRate()).isEqualByComparingTo("0.50");
        assertThat(calculation.amount()).isEqualByComparingTo("1.00");
        assertThat(calculation.currency()).isEqualTo("EUR");
    }

    @Test
    void roundsAnyStartedOverduePeriodUpToTheNextDay() {
        LateFeePolicy.Calculation calculation = policy.calculate(
                DUE_AT,
                DUE_AT.plusSeconds(24L * 60 * 60 + 1)
        ).orElseThrow();

        assertThat(calculation.daysLate()).isEqualTo(2);
        assertThat(calculation.amount()).isEqualByComparingTo("1.00");
    }
}
