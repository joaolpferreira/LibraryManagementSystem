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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class AssistantService {

    private static final int RESULT_LIMIT = 5;
    private static final String RECOMMEND_BOOK_SUGGESTION = "Recommend a book for me";
    private static final String SHOW_MY_LOANS_SUGGESTION = "Show my loans";

    private final AssistantIntentClassifier classifier;
    private final NaturalLanguageSearchService searchService;
    private final RecommendationService recommendationService;
    private final LoanService loanService;
    private final ReservationService reservationService;
    private final LateFeeService lateFeeService;

    public AssistantService(
            AssistantIntentClassifier classifier,
            NaturalLanguageSearchService searchService,
            RecommendationService recommendationService,
            LoanService loanService,
            ReservationService reservationService,
            LateFeeService lateFeeService
    ) {
        this.classifier = classifier;
        this.searchService = searchService;
        this.recommendationService = recommendationService;
        this.loanService = loanService;
        this.reservationService = reservationService;
        this.lateFeeService = lateFeeService;
    }

    public AssistantResponse chat(String message, String username, boolean owner) {
        AssistantIntent intent = classifier.classify(message);
        if (intent == AssistantIntent.HELP) {
            return help(owner);
        }
        if (owner && intent != AssistantIntent.SEARCH_BOOKS) {
            return restricted();
        }
        return switch (intent) {
            case SEARCH_BOOKS -> search(message);
            case RECOMMENDATIONS -> recommendations(username);
            case MY_LOANS -> loans(username);
            case MY_RESERVATIONS -> reservations(username);
            case MY_LATE_FEES -> lateFees(username);
            case HELP, ROLE_RESTRICTED -> throw new IllegalStateException(
                    "Intent was handled before dispatch"
            );
        };
    }

    private AssistantResponse help(boolean owner) {
        List<String> suggestions = owner
                ? List.of(
                        "Find available books by Martin Fowler",
                        "Use the owner REST endpoints or MCP tools to manage inventory"
                )
                : List.of(
                        "Find available books by Martin Fowler",
                        RECOMMEND_BOOK_SUGGESTION,
                        SHOW_MY_LOANS_SUGGESTION,
                        "Show my reservations",
                        "Show my late fees"
                );
        return empty(
                AssistantIntent.HELP,
                "I can search the live catalog and explain your role-specific library data. "
                        + "State-changing operations remain explicit REST or MCP commands.",
                suggestions
        );
    }

    private AssistantResponse restricted() {
        return empty(
                AssistantIntent.ROLE_RESTRICTED,
                "That is a client-personal request. Owners can use the assistant to search "
                        + "the catalog and the explicit owner endpoints for administration.",
                List.of("Find all unavailable books", "Help")
        );
    }

    private AssistantResponse search(String message) {
        NaturalLanguageSearchResponse result = searchService.search(
                message,
                PageRequest.of(0, RESULT_LIMIT, Sort.by("title").ascending())
        );
        return new AssistantResponse(
                AssistantIntent.SEARCH_BOOKS,
                "I found " + result.page().totalElements() + " matching catalog item(s).",
                result,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(RECOMMEND_BOOK_SUGGESTION, "Show only available books")
        );
    }

    private AssistantResponse recommendations(String username) {
        List<RecommendationResponse> result = recommendationService.recommend(
                username,
                RESULT_LIMIT
        );
        return new AssistantResponse(
                AssistantIntent.RECOMMENDATIONS,
                "These recommendations are ranked from your borrowing history, subjects, "
                        + "catalog popularity, and current availability.",
                null,
                result,
                List.of(),
                List.of(),
                List.of(),
                List.of(SHOW_MY_LOANS_SUGGESTION, "Find available books")
        );
    }

    private AssistantResponse loans(String username) {
        List<LoanResponse> result = loanService.myHistory(
                username,
                PageRequest.of(0, RESULT_LIMIT)
        ).getContent();
        return new AssistantResponse(
                AssistantIntent.MY_LOANS,
                "Here are your most recent loans.",
                null,
                List.of(),
                result,
                List.of(),
                List.of(),
                List.of("Show my late fees", RECOMMEND_BOOK_SUGGESTION)
        );
    }

    private AssistantResponse reservations(String username) {
        List<ReservationResponse> result = reservationService.myReservations(
                username,
                null,
                PageRequest.of(0, RESULT_LIMIT)
        ).getContent();
        return new AssistantResponse(
                AssistantIntent.MY_RESERVATIONS,
                "Here are your current and previous reservations.",
                null,
                List.of(),
                List.of(),
                result,
                List.of(),
                List.of(SHOW_MY_LOANS_SUGGESTION, "Find unavailable books")
        );
    }

    private AssistantResponse lateFees(String username) {
        List<LateFeeResponse> result = lateFeeService.myFees(
                username,
                null,
                PageRequest.of(0, RESULT_LIMIT)
        ).getContent();
        return new AssistantResponse(
                AssistantIntent.MY_LATE_FEES,
                "Here are your late-fee records. Payment and waiver decisions remain owner actions.",
                null,
                List.of(),
                List.of(),
                List.of(),
                result,
                List.of(SHOW_MY_LOANS_SUGGESTION, "Help")
        );
    }

    private static AssistantResponse empty(
            AssistantIntent intent,
            String reply,
            List<String> suggestions
    ) {
        return new AssistantResponse(
                intent,
                reply,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                suggestions
        );
    }
}
