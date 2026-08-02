package com.example.library.assistant;

import java.util.List;

import com.example.library.fee.LateFeeResponse;
import com.example.library.fee.LateFeeService;
import com.example.library.loan.LoanResponse;
import com.example.library.loan.LoanService;
import com.example.library.recommendation.RecommendationResponse;
import com.example.library.recommendation.RecommendationService;
import com.example.library.reservation.ReservationResponse;
import com.example.library.reservation.ReservationService;
import com.example.library.search.NaturalLanguageSearchResponse;
import com.example.library.search.NaturalLanguageSearchService;
import com.example.library.search.PageSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

    @Mock
    private AssistantIntentClassifier classifier;
    @Mock
    private NaturalLanguageSearchService searchService;
    @Mock
    private RecommendationService recommendationService;
    @Mock
    private LoanService loanService;
    @Mock
    private ReservationService reservationService;
    @Mock
    private LateFeeService lateFeeService;

    private AssistantService service;

    @BeforeEach
    void setUp() {
        service = new AssistantService(
                classifier,
                searchService,
                recommendationService,
                loanService,
                reservationService,
                lateFeeService
        );
    }

    @Test
    void providesRoleAwareHelpAndRejectsOwnerPersonalIntents() {
        when(classifier.classify("help")).thenReturn(AssistantIntent.HELP);
        AssistantResponse clientHelp = service.chat("help", "client", false);
        AssistantResponse ownerHelp = service.chat("help", "owner", true);

        assertThat(clientHelp.intent()).isEqualTo(AssistantIntent.HELP);
        assertThat(clientHelp.suggestions()).hasSize(5).isUnmodifiable();
        assertThat(ownerHelp.suggestions()).hasSize(2);

        when(classifier.classify("recommend")).thenReturn(AssistantIntent.RECOMMENDATIONS);
        AssistantResponse restricted = service.chat("recommend", "owner", true);
        assertThat(restricted.intent()).isEqualTo(AssistantIntent.ROLE_RESTRICTED);
        assertThat(restricted.reply()).contains("client-personal");
    }

    @Test
    void dispatchesSearchAndRecommendationIntents() {
        NaturalLanguageSearchResponse search = new NaturalLanguageSearchResponse(
                null, List.of(), new PageSummary(0, 5, 3, 1)
        );
        when(classifier.classify("find clean code")).thenReturn(AssistantIntent.SEARCH_BOOKS);
        when(searchService.search(eq("find clean code"), any())).thenReturn(search);
        AssistantResponse searchResponse = service.chat("find clean code", "client", false);
        assertThat(searchResponse.search()).isSameAs(search);
        assertThat(searchResponse.reply()).contains("3 matching");
        assertThat(service.chat("find clean code", "owner", true).search()).isSameAs(search);

        RecommendationResponse recommendation = mock(RecommendationResponse.class);
        when(classifier.classify("recommend")).thenReturn(AssistantIntent.RECOMMENDATIONS);
        when(recommendationService.recommend("client", 5))
                .thenReturn(List.of(recommendation));
        AssistantResponse recommendationResponse = service.chat("recommend", "client", false);
        assertThat(recommendationResponse.recommendations()).containsExactly(recommendation);
    }

    @Test
    void dispatchesEveryPersonalHistoryIntent() {
        LoanResponse loan = mock(LoanResponse.class);
        ReservationResponse reservation = mock(ReservationResponse.class);
        LateFeeResponse fee = mock(LateFeeResponse.class);
        when(loanService.myHistory(eq("client"), any()))
                .thenReturn(new PageImpl<>(List.of(loan)));
        when(reservationService.myReservations(eq("client"), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(reservation)));
        when(lateFeeService.myFees(eq("client"), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(fee)));

        when(classifier.classify("loans")).thenReturn(AssistantIntent.MY_LOANS);
        assertThat(service.chat("loans", "client", false).loans()).containsExactly(loan);

        when(classifier.classify("reservations")).thenReturn(AssistantIntent.MY_RESERVATIONS);
        assertThat(service.chat("reservations", "client", false).reservations())
                .containsExactly(reservation);

        when(classifier.classify("fees")).thenReturn(AssistantIntent.MY_LATE_FEES);
        assertThat(service.chat("fees", "client", false).lateFees()).containsExactly(fee);
    }

    @Test
    void failsClosedForAnInvalidInternalDispatchState() {
        when(classifier.classify("unexpected")).thenReturn(AssistantIntent.ROLE_RESTRICTED);

        assertThatThrownBy(() -> service.chat("unexpected", "client", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Intent was handled before dispatch");
    }
}
