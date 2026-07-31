package com.example.library.book;

public record BookResponse(
        Long id,
        String isbn,
        String title,
        String author,
        String description,
        int totalCopies,
        int availableCopies,
        boolean available
) {
    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.getDescription(),
                book.getTotalCopies(),
                book.getAvailableCopies(),
                book.getAvailableCopies() > 0
        );
    }
}

