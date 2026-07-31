package com.example.library.loan;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    boolean existsByBookIdAndBorrowerIdAndReturnedAtIsNull(Long bookId, Long borrowerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select loan
            from Loan loan
            join fetch loan.book
            join fetch loan.borrower
            where loan.id = :id
            """)
    Optional<Loan> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"book", "borrower"})
    @Query("select loan from Loan loan where loan.id = :id")
    Optional<Loan> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"book", "borrower"})
    Page<Loan> findByBorrowerUsernameOrderByBorrowedAtDesc(String username, Pageable pageable);

    @EntityGraph(attributePaths = {"book", "borrower"})
    Page<Loan> findAllByOrderByBorrowedAtDesc(Pageable pageable);
}
