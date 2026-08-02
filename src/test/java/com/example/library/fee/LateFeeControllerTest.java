package com.example.library.fee;

import java.math.BigDecimal;
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
class LateFeeControllerTest {

    @Mock
    private LateFeeService service;

    private LateFeeController controller;
    private LateFeeResponse response;

    @BeforeEach
    void setUp() {
        controller = new LateFeeController(service);
        response = new LateFeeResponse(
                1L,
                10L,
                new LateFeeResponse.BookSummary(3L, "9780132350884", "Clean Code"),
                new LateFeeResponse.BorrowerSummary("client", "Demo Client"),
                2,
                new BigDecimal("0.50"),
                new BigDecimal("1.00"),
                "EUR",
                LateFeeStatus.OUTSTANDING,
                null,
                null,
                null
        );
    }

    @Test
    void delegatesClientAndOwnerLists() {
        Authentication client = authentication("client", "ROLE_CLIENT");
        PageRequest pageable = PageRequest.of(0, 20);
        when(service.myFees("client", LateFeeStatus.OUTSTANDING, pageable)).thenReturn(Page.empty());
        when(service.allFees(null, pageable)).thenReturn(Page.empty());

        assertThat(controller.myFees(client, LateFeeStatus.OUTSTANDING, pageable)).isEmpty();
        assertThat(controller.allFees(null, pageable)).isEmpty();
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
    void delegatesSettlement() {
        LateFeeSettlementRequest request = new LateFeeSettlementRequest(
                LateFeeSettlementAction.PAID,
                "Receipt 42"
        );
        when(service.settle(1L, request)).thenReturn(response);

        assertThat(controller.settle(1L, request)).isEqualTo(response);
    }

    private static Authentication authentication(String username, String authority) {
        return new TestingAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority(authority))
        );
    }
}
