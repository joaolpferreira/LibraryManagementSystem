package com.example.library.config;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "library.late-fee")
public record LateFeeProperties(
        @NotNull @DecimalMin("0.01") @Digits(integer = 7, fraction = 2) BigDecimal dailyRate,
        @NotNull @Pattern(regexp = "[A-Z]{3}") String currency
) {
}
