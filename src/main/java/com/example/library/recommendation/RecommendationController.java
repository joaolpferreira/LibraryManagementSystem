package com.example.library.recommendation;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT')")
    public List<RecommendationResponse> myRecommendations(
            Authentication authentication,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit
    ) {
        return recommendationService.recommend(authentication.getName(), limit);
    }
}
