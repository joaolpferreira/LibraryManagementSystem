package com.example.library.loan;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<LoanResponse> borrow(
            @Valid @RequestBody BorrowBookRequest request,
            Authentication authentication
    ) {
        LoanResponse created = loanService.borrow(request, authentication.getName());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{loanId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'OWNER')")
    public LoanResponse get(@PathVariable Long loanId, Authentication authentication) {
        boolean owner = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_OWNER"));
        return loanService.get(loanId, authentication.getName(), owner);
    }

    @PostMapping("/{loanId}/return")
    @PreAuthorize("hasRole('CLIENT')")
    public LoanResponse returnBook(@PathVariable Long loanId, Authentication authentication) {
        return loanService.returnBook(loanId, authentication.getName());
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT')")
    public Page<LoanResponse> myHistory(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return loanService.myHistory(authentication.getName(), pageable);
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('OWNER')")
    public Page<LoanResponse> allHistory(@PageableDefault(size = 20) Pageable pageable) {
        return loanService.allHistory(pageable);
    }
}
