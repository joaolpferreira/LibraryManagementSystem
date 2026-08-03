package com.example.library.assistant;

import java.util.List;

import com.example.library.fee.LateFeeResponse;
import com.example.library.loan.LoanResponse;
import com.example.library.recommendation.RecommendationResponse;
import com.example.library.reservation.ReservationResponse;
import com.example.library.search.NaturalLanguageSearchResponse;

public record AssistantResponse(
        AssistantIntent intent,
        String reply,
        NaturalLanguageSearchResponse search,
        List<RecommendationResponse> recommendations,
        List<LoanResponse> loans,
        List<ReservationResponse> reservations,
        List<LateFeeResponse> lateFees,
        List<String> suggestions
) {
    public AssistantResponse {
        recommendations = List.copyOf(recommendations);
        loans = List.copyOf(loans);
        reservations = List.copyOf(reservations);
        lateFees = List.copyOf(lateFees);
        suggestions = List.copyOf(suggestions);
    }
}
