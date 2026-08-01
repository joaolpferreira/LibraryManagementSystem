package com.example.library.book;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("""
            select book
            from Book book
            where book.active = true
              and (
                :query = ''
                or lower(book.title) like lower(concat('%', :query, '%'))
                or lower(book.author) like lower(concat('%', :query, '%'))
                or lower(book.isbn) like lower(concat('%', :query, '%'))
              )
              and (
                :availabilityFilter = -1
                or (:availabilityFilter = 1 and book.availableCopies > 0)
                or (:availabilityFilter = 0 and book.availableCopies = 0)
              )
            """)
    Page<Book> search(
            @Param("query") String query,
            @Param("availabilityFilter") int availabilityFilter,
            Pageable pageable
    );

    Optional<Book> findByIdAndActiveTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select book from Book book where book.id = :id and book.active = true")
    Optional<Book> findActiveByIdForUpdate(@Param("id") Long id);

    boolean existsByIsbnIgnoreCase(String isbn);

    boolean existsByIsbnIgnoreCaseAndIdNot(String isbn, Long id);
}
