package com.example.library.reservation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookReservationRepository extends JpaRepository<BookReservation, Long> {

    boolean existsByActiveKey(String activeKey);

    boolean existsByActiveKeyStartingWith(String activeKeyPrefix);

    long countByBookIdAndStatus(Long bookId, ReservationStatus status);

    @EntityGraph(attributePaths = {"book", "borrower"})
    @Query("select reservation from BookReservation reservation where reservation.id = :id")
    Optional<BookReservation> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"book", "borrower"})
    @Query("select reservation from BookReservation reservation where reservation.id = :id")
    Optional<BookReservation> findDetailedByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"book", "borrower"})
    @Query("""
            select reservation
            from BookReservation reservation
            where reservation.book.id = :bookId
              and reservation.borrower.id = :borrowerId
              and reservation.activeKey is not null
            """)
    Optional<BookReservation> findActiveForBorrowerForUpdate(
            @Param("bookId") Long bookId,
            @Param("borrowerId") Long borrowerId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"book", "borrower"})
    @Query("""
            select reservation
            from BookReservation reservation
            where reservation.book.id = :bookId
              and reservation.status = com.example.library.reservation.ReservationStatus.READY
            order by reservation.readyAt, reservation.id
            """)
    List<BookReservation> findReadyForBookForUpdate(@Param("bookId") Long bookId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"book", "borrower"})
    @Query("""
            select reservation
            from BookReservation reservation
            where reservation.book.id = :bookId
              and reservation.status = com.example.library.reservation.ReservationStatus.WAITING
            order by reservation.queuedAt, reservation.id
            """)
    List<BookReservation> findWaitingForBookForUpdate(
            @Param("bookId") Long bookId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"book", "borrower"})
    @Query("""
            select reservation
            from BookReservation reservation
            where reservation.borrower.username = :username
              and (:status is null or reservation.status = :status)
            order by reservation.queuedAt desc
            """)
    Page<BookReservation> findForBorrower(
            @Param("username") String username,
            @Param("status") ReservationStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"book", "borrower"})
    @Query("""
            select reservation
            from BookReservation reservation
            where :status is null or reservation.status = :status
            order by reservation.queuedAt desc
            """)
    Page<BookReservation> findAllDetailed(
            @Param("status") ReservationStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"book", "borrower"})
    @Query("""
            select reservation
            from BookReservation reservation
            where reservation.book.id = :bookId
              and reservation.activeKey is not null
            order by reservation.queuedAt, reservation.id
            """)
    Page<BookReservation> findActiveQueueForBook(
            @Param("bookId") Long bookId,
            Pageable pageable
    );

    @Query("""
            select count(reservation)
            from BookReservation reservation
            where reservation.book.id = :bookId
              and reservation.status = com.example.library.reservation.ReservationStatus.WAITING
              and (
                reservation.queuedAt < :queuedAt
                or (reservation.queuedAt = :queuedAt and reservation.id < :id)
              )
            """)
    long countWaitingAhead(
            @Param("bookId") Long bookId,
            @Param("queuedAt") Instant queuedAt,
            @Param("id") Long id
    );

    @Query("""
            select distinct reservation.book.id
            from BookReservation reservation
            where reservation.status = com.example.library.reservation.ReservationStatus.READY
              and reservation.expiresAt <= :now
            """)
    List<Long> findBookIdsWithExpiredReadyReservations(@Param("now") Instant now);
}
