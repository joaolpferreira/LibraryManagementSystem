package com.example.library.reservation;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReservationExpiryJobTest {

    @Test
    void delegatesScheduledExpiryReconciliation() {
        ReservationService service = mock(ReservationService.class);

        new ReservationExpiryJob(service).expireReadyReservations();

        verify(service).reconcileExpiredReservations();
    }
}
