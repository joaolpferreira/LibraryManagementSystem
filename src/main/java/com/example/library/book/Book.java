package com.example.library.book;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 150)
    private String author;

    @Column(length = 2_000)
    private String description;

    @Column(name = "total_copies", nullable = false)
    private int totalCopies;

    @Column(name = "available_copies", nullable = false)
    private int availableCopies;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Book() {
    }

    public Book(String isbn, String title, String author, String description, int totalCopies) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.description = description;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void update(String isbn, String title, String author, String description, int newTotalCopies) {
        int borrowedCopies = totalCopies - availableCopies;
        if (newTotalCopies < borrowedCopies) {
            throw new IllegalArgumentException(
                    "Total copies cannot be lower than the number of copies currently on loan"
            );
        }
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.description = description;
        this.totalCopies = newTotalCopies;
        this.availableCopies = newTotalCopies - borrowedCopies;
    }

    public void borrowCopy() {
        if (!active || availableCopies == 0) {
            throw new IllegalStateException("No copy of this book is available");
        }
        availableCopies--;
    }

    public void returnCopy() {
        if (availableCopies >= totalCopies) {
            throw new IllegalStateException("All copies are already in the inventory");
        }
        availableCopies++;
    }

    public void deactivate() {
        if (availableCopies != totalCopies) {
            throw new IllegalStateException("A book with active loans cannot be removed");
        }
        active = false;
    }

    public Long getId() {
        return id;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public boolean isActive() {
        return active;
    }
}

