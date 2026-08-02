package com.example.library.fee;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import com.example.library.config.LateFeeProperties;
import org.springframework.stereotype.Component;

@Component
public class LateFeePolicy {

    private final BigDecimal dailyRate;
    private final String currency;

    public LateFeePolicy(LateFeeProperties properties) {
        dailyRate = properties.dailyRate().setScale(2, RoundingMode.UNNECESSARY);
        currency = properties.currency();
    }

    public Optional<Calculation> calculate(Instant dueAt, Instant returnedAt) {
        if (!returnedAt.isAfter(dueAt)) {
            return Optional.empty();
        }

        Duration overdue = Duration.between(dueAt, returnedAt);
        long completeDays = overdue.toDays();
        long chargedDays = overdue.minusDays(completeDays).isZero()
                ? completeDays
                : Math.addExact(completeDays, 1);
        int daysLate = Math.toIntExact(chargedDays);
        BigDecimal amount = dailyRate.multiply(BigDecimal.valueOf(daysLate));
        return Optional.of(new Calculation(daysLate, dailyRate, amount, currency));
    }

    public record Calculation(
            int daysLate,
            BigDecimal dailyRate,
            BigDecimal amount,
            String currency
    ) {
    }
}
