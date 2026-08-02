package com.example.library.fee;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/late-fees")
public class LateFeeController {

    private final LateFeeService lateFeeService;

    public LateFeeController(LateFeeService lateFeeService) {
        this.lateFeeService = lateFeeService;
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT')")
    public Page<LateFeeResponse> myFees(
            Authentication authentication,
            @RequestParam(required = false) LateFeeStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return lateFeeService.myFees(authentication.getName(), status, pageable);
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public Page<LateFeeResponse> allFees(
            @RequestParam(required = false) LateFeeStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return lateFeeService.allFees(status, pageable);
    }

    @GetMapping("/{feeId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'OWNER')")
    public LateFeeResponse get(@PathVariable Long feeId, Authentication authentication) {
        boolean owner = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_OWNER"));
        return lateFeeService.get(feeId, authentication.getName(), owner);
    }

    @PostMapping("/{feeId}/settlement")
    @PreAuthorize("hasRole('OWNER')")
    public LateFeeResponse settle(
            @PathVariable Long feeId,
            @Valid @RequestBody LateFeeSettlementRequest request
    ) {
        return lateFeeService.settle(feeId, request);
    }
}
