package com.example.library.reservation;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpiryJob {

    private final ReservationService reservationService;

    public ReservationExpiryJob(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(fixedDelayString = "${library.reservation.expiration-scan-delay:1m}")
    public void expireReadyReservations() {
        reservationService.reconcileExpiredReservations();
    }
}
