package com.example.library.mcp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpAuthenticationTest {

    private final McpAuthentication authentication = new McpAuthentication();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsAuthenticatedUsername() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "client",
                        "password",
                        AuthorityUtils.createAuthorityList("ROLE_CLIENT")
                )
        );

        assertThat(authentication.username()).isEqualTo("client");
    }

    @Test
    void rejectsMissingAuthentication() {
        assertRejected();
    }

    @Test
    void rejectsUnauthenticatedToken() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated("client", "password")
        );

        assertRejected();
    }

    @Test
    void rejectsAnonymousAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken(
                        "key",
                        "anonymous",
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
                )
        );

        assertRejected();
    }

    private void assertRejected() {
        assertThatThrownBy(authentication::username)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("An authenticated library user is required");
    }
}
