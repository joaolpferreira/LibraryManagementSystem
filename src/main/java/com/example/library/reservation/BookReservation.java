package com.example.library.reservation;

import java.time.Instant;

import com.example.library.book.Book;
import com.example.library.user.LibraryUser;
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
@Table(name = "book_reservations")
public class BookReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false, updatable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrower_id", nullable = false, updatable = false)
    private LibraryUser borrower;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.WAITING;

    @Column(name = "active_key", unique = true, length = 50)
    private String activeKey;

    @Column(name = "queued_at", nullable = false, updatable = false)
    private Instant queuedAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "fulfilled_at")
    private Instant fulfilledAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected BookReservation() {
    }

    public BookReservation(Book book, LibraryUser borrower, Instant queuedAt) {
        this.book = book;
        this.borrower = borrower;
        this.queuedAt = queuedAt;
        activeKey = activeKey(book.getId(), borrower.getId());
    }

    public void markReady(Instant readyTime, Instant expiryTime) {
        if (status != ReservationStatus.WAITING) {
            throw new IllegalStateException("Only a waiting reservation can become ready");
        }
        status = ReservationStatus.READY;
        readyAt = readyTime;
        expiresAt = expiryTime;
    }

    public void fulfill(Instant fulfillmentTime) {
        if (status != ReservationStatus.READY) {
            throw new IllegalStateException("Only a ready reservation can be fulfilled");
        }
        status = ReservationStatus.FULFILLED;
        fulfilledAt = fulfillmentTime;
        activeKey = null;
    }

    public void cancel(Instant cancellationTime) {
        if (status != ReservationStatus.WAITING && status != ReservationStatus.READY) {
            throw new IllegalStateException("Only an active reservation can be cancelled");
        }
        status = ReservationStatus.CANCELLED;
        cancelledAt = cancellationTime;
        activeKey = null;
    }

    public void expire(Instant expirationTime) {
        if (status != ReservationStatus.READY) {
            throw new IllegalStateException("Only a ready reservation can expire");
        }
        status = ReservationStatus.EXPIRED;
        expiredAt = expirationTime;
        activeKey = null;
    }

    public boolean isReadyAndExpiredAt(Instant now) {
        return status == ReservationStatus.READY && !expiresAt.isAfter(now);
    }

    static String activeKey(Long bookId, Long borrowerId) {
        return bookId + ":" + borrowerId;
    }

    public Long getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public LibraryUser getBorrower() {
        return borrower;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getQueuedAt() {
        return queuedAt;
    }

    public Instant getReadyAt() {
        return readyAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getFulfilledAt() {
        return fulfilledAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }
}
