package com.example.library.reservation;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ReservationResponse> reserve(
            @Valid @RequestBody ReserveBookRequest request,
            Authentication authentication
    ) {
        ReservationResponse created = reservationService.reserve(request, authentication.getName());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT')")
    public Page<ReservationResponse> myReservations(
            Authentication authentication,
            @RequestParam(required = false) ReservationStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return reservationService.myReservations(authentication.getName(), status, pageable);
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public Page<ReservationResponse> allReservations(
            @RequestParam(required = false) ReservationStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return reservationService.allReservations(status, pageable);
    }

    @GetMapping("/books/{bookId}/queue")
    @PreAuthorize("hasRole('OWNER')")
    public Page<ReservationResponse> queueForBook(
            @PathVariable Long bookId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return reservationService.queueForBook(bookId, pageable);
    }

    @GetMapping("/{reservationId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'OWNER')")
    public ReservationResponse get(
            @PathVariable Long reservationId,
            Authentication authentication
    ) {
        boolean owner = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_OWNER"));
        return reservationService.get(reservationId, authentication.getName(), owner);
    }

    @PostMapping("/{reservationId}/cancel")
    @PreAuthorize("hasRole('CLIENT')")
    public ReservationResponse cancel(
            @PathVariable Long reservationId,
            Authentication authentication
    ) {
        return reservationService.cancel(reservationId, authentication.getName());
    }
}
