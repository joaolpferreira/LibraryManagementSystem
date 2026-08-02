package com.example.library.reservation;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationControllerTest {

    @Mock
    private ReservationService service;

    private ReservationController controller;
    private ReservationResponse response;

    @BeforeEach
    void setUp() {
        controller = new ReservationController(service);
        response = new ReservationResponse(
                1L,
                new ReservationResponse.BookSummary(2L, "9780132350884", "Clean Code"),
                new ReservationResponse.BorrowerSummary("client", "Demo Client"),
                ReservationStatus.WAITING,
                1L,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void delegatesListsAndBookQueue() {
        Authentication client = authentication("client", "ROLE_CLIENT");
        PageRequest pageable = PageRequest.of(0, 20);
        when(service.myReservations("client", ReservationStatus.WAITING, pageable))
                .thenReturn(Page.empty());
        when(service.allReservations(null, pageable)).thenReturn(Page.empty());
        when(service.queueForBook(2L, pageable)).thenReturn(Page.empty());

        assertThat(controller.myReservations(client, ReservationStatus.WAITING, pageable)).isEmpty();
        assertThat(controller.allReservations(null, pageable)).isEmpty();
        assertThat(controller.queueForBook(2L, pageable)).isEmpty();
    }

    @Test
    void getDetectsClientAndOwnerAuthorities() {
        Authentication client = authentication("client", "ROLE_CLIENT");
        Authentication owner = authentication("owner", "ROLE_OWNER");
        when(service.get(1L, "client", false)).thenReturn(response);
        when(service.get(1L, "owner", true)).thenReturn(response);

        assertThat(controller.get(1L, client)).isEqualTo(response);
        assertThat(controller.get(1L, owner)).isEqualTo(response);
    }

    @Test
    void delegatesCancellation() {
        Authentication client = authentication("client", "ROLE_CLIENT");
        when(service.cancel(1L, "client")).thenReturn(response);

        assertThat(controller.cancel(1L, client)).isEqualTo(response);
    }

    private static Authentication authentication(String username, String authority) {
        return new TestingAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority(authority))
        );
    }
}
