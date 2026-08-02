package com.example.library.fee;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.library.loan.Loan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "late_fees")
public class LateFee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false, unique = true, updatable = false)
    private Loan loan;

    @Column(name = "days_late", nullable = false, updatable = false)
    private int daysLate;

    @Column(name = "daily_rate", nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal dailyRate;

    @Column(nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LateFeeStatus status = LateFeeStatus.OUTSTANDING;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Column(name = "settlement_note", length = 500)
    private String settlementNote;

    @Version
    @Column(nullable = false)
    private long version;

    protected LateFee() {
    }

    public LateFee(Loan loan, LateFeePolicy.Calculation calculation, Instant registeredAt) {
        this.loan = loan;
        daysLate = calculation.daysLate();
        dailyRate = calculation.dailyRate();
        amount = calculation.amount();
        currency = calculation.currency();
        this.registeredAt = registeredAt;
    }

    public void settle(LateFeeSettlementAction action, Instant settlementTime, String note) {
        if (status != LateFeeStatus.OUTSTANDING) {
            throw new IllegalStateException("This late fee has already been settled");
        }
        status = LateFeeStatus.valueOf(action.name());
        settledAt = settlementTime;
        settlementNote = note == null || note.isBlank() ? null : note.trim();
    }

    public Long getId() {
        return id;
    }

    public Loan getLoan() {
        return loan;
    }

    public int getDaysLate() {
        return daysLate;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public LateFeeStatus getStatus() {
        return status;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public Instant getSettledAt() {
        return settledAt;
    }

    public String getSettlementNote() {
        return settlementNote;
    }
}
