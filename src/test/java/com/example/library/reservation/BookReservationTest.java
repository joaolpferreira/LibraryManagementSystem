package com.example.library.reservation;

import java.time.Instant;

import com.example.library.book.Book;
import com.example.library.user.LibraryUser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookReservationTest {

    private static final Instant QUEUED_AT = Instant.parse("2026-01-01T12:00:00Z");
    private static final Instant READY_AT = Instant.parse("2026-01-02T12:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-01-04T12:00:00Z");

    @Test
    void constructorsAndGettersExposeWaitingState() {
        BookReservation empty = new BookReservation();
        assertThat(empty.getId()).isNull();

        BookReservation reservation = reservation();
        assertThat(reservation.getId()).isNull();
        assertThat(reservation.getBook().getId()).isEqualTo(1L);
        assertThat(reservation.getBorrower().getUsername()).isEqualTo("client");
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.WAITING);
        assertThat(reservation.getQueuedAt()).isEqualTo(QUEUED_AT);
        assertThat(reservation.getReadyAt()).isNull();
        assertThat(reservation.getExpiresAt()).isNull();
        assertThat(reservation.getFulfilledAt()).isNull();
        assertThat(reservation.getCancelledAt()).isNull();
        assertThat(reservation.getExpiredAt()).isNull();
        assertThat(BookReservation.activeKey(1L, 2L)).isEqualTo("1:2");
    }

    @Test
    void waitingReservationCanBecomeReadyAndThenFulfilled() {
        BookReservation reservation = reservation();
        reservation.markReady(READY_AT, EXPIRES_AT);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.READY);
        assertThat(reservation.getReadyAt()).isEqualTo(READY_AT);
        assertThat(reservation.getExpiresAt()).isEqualTo(EXPIRES_AT);

        Instant fulfilledAt = READY_AT.plusSeconds(10);
        reservation.fulfill(fulfilledAt);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.FULFILLED);
        assertThat(reservation.getFulfilledAt()).isEqualTo(fulfilledAt);

        assertThatThrownBy(() -> reservation.markReady(READY_AT, EXPIRES_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("waiting reservation");
        assertThatThrownBy(() -> reservation.fulfill(fulfilledAt))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ready reservation");
    }

    @Test
    void waitingAndReadyReservationsCanBeCancelledButTerminalOnesCannot() {
        BookReservation waiting = reservation();
        waiting.cancel(READY_AT);
        assertThat(waiting.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(waiting.getCancelledAt()).isEqualTo(READY_AT);

        BookReservation ready = reservation();
        ready.markReady(READY_AT, EXPIRES_AT);
        ready.cancel(READY_AT.plusSeconds(1));
        assertThat(ready.getStatus()).isEqualTo(ReservationStatus.CANCELLED);

        Instant duplicateCancellationTime = READY_AT.plusSeconds(2);
        assertThatThrownBy(() -> ready.cancel(duplicateCancellationTime))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active reservation");
    }

    @Test
    void onlyReadyReservationsCanExpire() {
        BookReservation waiting = reservation();
        assertThatThrownBy(() -> waiting.expire(EXPIRES_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ready reservation");

        BookReservation ready = reservation();
        ready.markReady(READY_AT, EXPIRES_AT);
        ready.expire(EXPIRES_AT);

        assertThat(ready.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(ready.getExpiredAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    void expiryCheckRequiresReadyStatusAndElapsedDeadline() {
        BookReservation waiting = reservation();
        assertThat(waiting.isReadyAndExpiredAt(EXPIRES_AT)).isFalse();

        BookReservation ready = reservation();
        ready.markReady(READY_AT, EXPIRES_AT);
        assertThat(ready.isReadyAndExpiredAt(EXPIRES_AT.minusSeconds(1))).isFalse();
        assertThat(ready.isReadyAndExpiredAt(EXPIRES_AT)).isTrue();
    }

    @Test
    void responseMapsAllReservationDetails() {
        BookReservation reservation = reservation();
        reservation.markReady(READY_AT, EXPIRES_AT);

        ReservationResponse response = ReservationResponse.from(reservation, null);

        assertThat(response.book().title()).isEqualTo("Clean Code");
        assertThat(response.borrower().displayName()).isEqualTo("Demo Client");
        assertThat(response.status()).isEqualTo(ReservationStatus.READY);
        assertThat(response.expiresAt()).isEqualTo(EXPIRES_AT);
    }

    private static BookReservation reservation() {
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(1L);
        when(book.getIsbn()).thenReturn("9780132350884");
        when(book.getTitle()).thenReturn("Clean Code");
        LibraryUser borrower = mock(LibraryUser.class);
        when(borrower.getId()).thenReturn(2L);
        when(borrower.getUsername()).thenReturn("client");
        when(borrower.getDisplayName()).thenReturn("Demo Client");
        return new BookReservation(book, borrower, QUEUED_AT);
    }
}
