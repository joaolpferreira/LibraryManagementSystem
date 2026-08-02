package com.example.library.fee;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.example.library.book.Book;
import com.example.library.common.ConflictException;
import com.example.library.common.ResourceNotFoundException;
import com.example.library.loan.Loan;
import com.example.library.user.LibraryUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LateFeeServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-03T12:00:00Z");
    private static final LateFeePolicy.Calculation CALCULATION = new LateFeePolicy.Calculation(
            2,
            new BigDecimal("0.50"),
            new BigDecimal("1.00"),
            "EUR"
    );

    @Mock
    private LateFeeRepository lateFeeRepository;
    @Mock
    private LateFeePolicy lateFeePolicy;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private LateFeeService service;

    @BeforeEach
    void setUp() {
        service = new LateFeeService(
                lateFeeRepository,
                lateFeePolicy,
                eventPublisher,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void onTimeReturnDoesNotRegisterAFee() {
        Loan loan = loan("client");
        when(lateFeePolicy.calculate(loan.getDueAt(), NOW)).thenReturn(Optional.empty());

        assertThat(service.registerIfLate(loan, NOW)).isEmpty();
        verify(lateFeeRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void lateReturnPersistsSnapshotAndPublishesEvent() {
        Loan loan = loan("client");
        when(lateFeePolicy.calculate(loan.getDueAt(), NOW)).thenReturn(Optional.of(CALCULATION));
        when(lateFeeRepository.save(any(LateFee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LateFeeResponse response = service.registerIfLate(loan, NOW).orElseThrow();

        assertThat(response.daysLate()).isEqualTo(2);
        assertThat(response.amount()).isEqualByComparingTo("1.00");
        assertThat(response.status()).isEqualTo(LateFeeStatus.OUTSTANDING);
        ArgumentCaptor<LateFeeRegisteredEvent> event = ArgumentCaptor.forClass(LateFeeRegisteredEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().loanId()).isEqualTo(10L);
        assertThat(event.getValue().username()).isEqualTo("client");
        assertThat(event.getValue().registeredAt()).isEqualTo(NOW);
    }

    @Test
    void feeListsMapBorrowerAndOwnerRepositoryPages() {
        LateFee fee = fee("client");
        PageRequest pageable = PageRequest.of(0, 20);
        when(lateFeeRepository.findForBorrower("client", LateFeeStatus.OUTSTANDING, pageable))
                .thenReturn(new PageImpl<>(List.of(fee)));
        when(lateFeeRepository.findAllDetailed(null, pageable))
                .thenReturn(new PageImpl<>(List.of(fee)));

        assertThat(service.myFees("client", LateFeeStatus.OUTSTANDING, pageable).getContent())
                .singleElement().extracting(LateFeeResponse::currency).isEqualTo("EUR");
        assertThat(service.allFees(null, pageable).getContent())
                .singleElement().extracting(LateFeeResponse::daysLate).isEqualTo(2);
    }

    @Test
    void getEnforcesClientOwnershipAndAllowsOwners() {
        LateFee fee = fee("client");
        when(lateFeeRepository.findDetailedById(1L)).thenReturn(Optional.of(fee));
        when(lateFeeRepository.findDetailedById(2L)).thenReturn(Optional.empty());

        assertThat(service.get(1L, "client", false).borrower().username()).isEqualTo("client");
        assertThat(service.get(1L, "owner", true).borrower().username()).isEqualTo("client");
        assertThatThrownBy(() -> service.get(1L, "someone-else", false))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("only view their own");
        assertThatThrownBy(() -> service.get(2L, "client", false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Late fee 2 was not found");
    }

    @Test
    void ownerCanMarkAnOutstandingFeePaidOrWaived() {
        LateFee paid = fee("client");
        LateFee waived = fee("client");
        when(lateFeeRepository.findDetailedByIdForUpdate(1L)).thenReturn(Optional.of(paid));
        when(lateFeeRepository.findDetailedByIdForUpdate(2L)).thenReturn(Optional.of(waived));

        LateFeeResponse paidResponse = service.settle(
                1L,
                new LateFeeSettlementRequest(LateFeeSettlementAction.PAID, "  Cash receipt 42  ")
        );
        LateFeeResponse waivedResponse = service.settle(
                2L,
                new LateFeeSettlementRequest(LateFeeSettlementAction.WAIVED, null)
        );

        assertThat(paidResponse.status()).isEqualTo(LateFeeStatus.PAID);
        assertThat(paidResponse.settlementNote()).isEqualTo("Cash receipt 42");
        assertThat(paidResponse.settledAt()).isEqualTo(NOW);
        assertThat(waivedResponse.status()).isEqualTo(LateFeeStatus.WAIVED);
    }

    @Test
    void settlementHandlesMissingAndAlreadySettledFees() {
        LateFee settled = fee("client");
        settled.settle(LateFeeSettlementAction.PAID, NOW.minusSeconds(1), null);
        when(lateFeeRepository.findDetailedByIdForUpdate(1L)).thenReturn(Optional.empty());
        when(lateFeeRepository.findDetailedByIdForUpdate(2L)).thenReturn(Optional.of(settled));
        LateFeeSettlementRequest request = new LateFeeSettlementRequest(
                LateFeeSettlementAction.WAIVED,
                "Duplicate"
        );

        assertThatThrownBy(() -> service.settle(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.settle(2L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already been settled");
    }

    private static LateFee fee(String username) {
        return new LateFee(loan(username), CALCULATION, NOW);
    }

    private static Loan loan(String username) {
        Book book = org.mockito.Mockito.mock(Book.class);
        lenient().when(book.getId()).thenReturn(3L);
        lenient().when(book.getIsbn()).thenReturn("9780132350884");
        lenient().when(book.getTitle()).thenReturn("Clean Code");
        LibraryUser borrower = org.mockito.Mockito.mock(LibraryUser.class);
        lenient().when(borrower.getUsername()).thenReturn(username);
        lenient().when(borrower.getDisplayName()).thenReturn("Demo Client");
        Loan loan = org.mockito.Mockito.mock(Loan.class);
        lenient().when(loan.getId()).thenReturn(10L);
        lenient().when(loan.getBook()).thenReturn(book);
        lenient().when(loan.getBorrower()).thenReturn(borrower);
        lenient().when(loan.getDueAt()).thenReturn(NOW.minusSeconds(25L * 60 * 60));
        return loan;
    }
}
