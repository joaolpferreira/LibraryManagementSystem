package com.example.library.loan;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanControllerTest {

    @Mock
    private LoanService service;

    private LoanController controller;
    private LoanResponse response;

    @BeforeEach
    void setUp() {
        controller = new LoanController(service);
        response = new LoanResponse(
                1L,
                new LoanResponse.BookSummary(1L, "9780132350884", "Clean Code"),
                new LoanResponse.BorrowerSummary("client", "Demo Client"),
                null,
                null,
                null,
                false,
                LoanStatus.ACTIVE
        );
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
    void delegatesReturnAndBothHistoryViews() {
        Authentication client = authentication("client", "ROLE_CLIENT");
        PageRequest pageable = PageRequest.of(0, 20);
        when(service.returnBook(1L, "client")).thenReturn(response);
        when(service.myHistory("client", pageable)).thenReturn(Page.empty());
        when(service.allHistory(pageable)).thenReturn(Page.empty());

        assertThat(controller.returnBook(1L, client)).isEqualTo(response);
        assertThat(controller.myHistory(client, pageable)).isEmpty();
        assertThat(controller.allHistory(pageable)).isEmpty();
        verify(service).returnBook(1L, "client");
    }

    private static Authentication authentication(String username, String authority) {
        return new TestingAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority(authority))
        );
    }
}
