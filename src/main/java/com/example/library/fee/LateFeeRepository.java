package com.example.library.fee;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LateFeeRepository extends JpaRepository<LateFee, Long> {

    @EntityGraph(attributePaths = {"loan", "loan.book", "loan.borrower"})
    @Query("select fee from LateFee fee where fee.id = :id")
    Optional<LateFee> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"loan", "loan.book", "loan.borrower"})
    @Query("select fee from LateFee fee where fee.id = :id")
    Optional<LateFee> findDetailedByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"loan", "loan.book", "loan.borrower"})
    @Query("""
            select fee
            from LateFee fee
            where fee.loan.borrower.username = :username
              and (:status is null or fee.status = :status)
            order by fee.registeredAt desc
            """)
    Page<LateFee> findForBorrower(
            @Param("username") String username,
            @Param("status") LateFeeStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"loan", "loan.book", "loan.borrower"})
    @Query("""
            select fee
            from LateFee fee
            where :status is null or fee.status = :status
            order by fee.registeredAt desc
            """)
    Page<LateFee> findAllDetailed(
            @Param("status") LateFeeStatus status,
            Pageable pageable
    );
}
