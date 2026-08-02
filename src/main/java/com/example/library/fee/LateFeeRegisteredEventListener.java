package com.example.library.fee;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class LateFeeRegisteredEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LateFeeRegisteredEventListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLateFeeRegistered(LateFeeRegisteredEvent event) {
        LOGGER.info(
                "Late fee registered: feeId={}, loanId={}, username={}, daysLate={}, amount={} {}",
                event.lateFeeId(),
                event.loanId(),
                event.username(),
                event.daysLate(),
                event.amount(),
                event.currency()
        );
    }
}
