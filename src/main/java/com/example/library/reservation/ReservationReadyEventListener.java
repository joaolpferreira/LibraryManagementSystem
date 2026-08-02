package com.example.library.reservation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ReservationReadyEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationReadyEventListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReservationReady(ReservationReadyEvent event) {
        LOGGER.info(
                "Reservation ready: reservationId={}, bookId={}, username={}, expiresAt={}",
                event.reservationId(),
                event.bookId(),
                event.username(),
                event.expiresAt()
        );
    }
}
