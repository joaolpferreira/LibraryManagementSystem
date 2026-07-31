package com.example.library.loan;

import java.time.Instant;

import com.example.library.book.Book;
import com.example.library.user.LibraryUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrower_id", nullable = false)
    private LibraryUser borrower;

    @Column(name = "borrowed_at", nullable = false, updatable = false)
    private Instant borrowedAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Column(name = "returned_late", nullable = false)
    private boolean returnedLate;

    protected Loan() {
    }

    public Loan(Book book, LibraryUser borrower, Instant borrowedAt, Instant dueAt) {
        this.book = book;
        this.borrower = borrower;
        this.borrowedAt = borrowedAt;
        this.dueAt = dueAt;
    }

    public void returnBook(Instant returnTime) {
        if (returnedAt != null) {
            throw new IllegalStateException("This loan has already been returned");
        }
        returnedAt = returnTime;
        returnedLate = returnTime.isAfter(dueAt);
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

    public Instant getBorrowedAt() {
        return borrowedAt;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }

    public boolean isReturnedLate() {
        return returnedLate;
    }

    public boolean isActive() {
        return returnedAt == null;
    }
}

