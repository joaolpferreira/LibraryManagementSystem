package com.example.library.recommendation;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationControllerTest {

    @Mock
    private RecommendationService service;

    @Test
    void delegatesUsingTheAuthenticatedIdentity() {
        RecommendationController controller = new RecommendationController(service);
        RecommendationResponse result = mock(RecommendationResponse.class);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "client", "password", List.of()
        );
        when(service.recommend("client", 7)).thenReturn(List.of(result));

        assertThat(controller.myRecommendations(authentication, 7)).containsExactly(result);
    }
}
