package com.example.library.assistant;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('CLIENT', 'OWNER')")
    public AssistantResponse chat(
            @Valid @RequestBody AssistantRequest request,
            Authentication authentication
    ) {
        boolean owner = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_OWNER"));
        return assistantService.chat(request.message().trim(), authentication.getName(), owner);
    }
}
