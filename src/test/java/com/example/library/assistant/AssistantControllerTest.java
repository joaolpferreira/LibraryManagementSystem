package com.example.library.assistant;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantControllerTest {

    @Mock
    private AssistantService service;

    @Test
    void delegatesClientAndOwnerMessagesWithTheirIdentity() {
        AssistantController controller = new AssistantController(service);
        AssistantResponse response = mock(AssistantResponse.class);
        var client = UsernamePasswordAuthenticationToken.authenticated(
                "client", "password", List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))
        );
        var owner = UsernamePasswordAuthenticationToken.authenticated(
                "owner", "password", List.of(new SimpleGrantedAuthority("ROLE_OWNER"))
        );
        AssistantRequest request = new AssistantRequest("  help  ");
        when(service.chat("help", "client", false)).thenReturn(response);
        when(service.chat("help", "owner", true)).thenReturn(response);

        assertThat(controller.chat(request, client)).isSameAs(response);
        assertThat(controller.chat(request, owner)).isSameAs(response);
    }
}
