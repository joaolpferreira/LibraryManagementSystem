package com.example.library.fee;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import com.example.library.common.ConflictException;
import com.example.library.common.ResourceNotFoundException;
import com.example.library.loan.Loan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LateFeeService {

    private final LateFeeRepository lateFeeRepository;
    private final LateFeePolicy lateFeePolicy;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public LateFeeService(
            LateFeeRepository lateFeeRepository,
            LateFeePolicy lateFeePolicy,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.lateFeeRepository = lateFeeRepository;
        this.lateFeePolicy = lateFeePolicy;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<LateFeeResponse> registerIfLate(Loan loan, Instant returnedAt) {
        return lateFeePolicy.calculate(loan.getDueAt(), returnedAt)
                .map(calculation -> register(loan, returnedAt, calculation));
    }

    @Transactional(readOnly = true)
    public Page<LateFeeResponse> myFees(
            String username,
            LateFeeStatus status,
            Pageable pageable
    ) {
        return lateFeeRepository.findForBorrower(username, status, pageable)
                .map(LateFeeResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<LateFeeResponse> allFees(LateFeeStatus status, Pageable pageable) {
        return lateFeeRepository.findAllDetailed(status, pageable)
                .map(LateFeeResponse::from);
    }

    @Transactional(readOnly = true)
    public LateFeeResponse get(Long feeId, String username, boolean owner) {
        LateFee fee = lateFeeRepository.findDetailedById(feeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Late fee " + feeId + " was not found"
                ));
        if (!owner && !fee.getLoan().getBorrower().getUsername().equals(username)) {
            throw new AccessDeniedException("A client can only view their own late fees");
        }
        return LateFeeResponse.from(fee);
    }

    @Transactional
    public LateFeeResponse settle(Long feeId, LateFeeSettlementRequest request) {
        LateFee fee = lateFeeRepository.findDetailedByIdForUpdate(feeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Late fee " + feeId + " was not found"
                ));
        try {
            fee.settle(request.action(), clock.instant(), request.note());
        } catch (IllegalStateException exception) {
            throw new ConflictException(exception.getMessage());
        }
        return LateFeeResponse.from(fee);
    }

    private LateFeeResponse register(
            Loan loan,
            Instant returnedAt,
            LateFeePolicy.Calculation calculation
    ) {
        LateFee fee = lateFeeRepository.save(new LateFee(loan, calculation, returnedAt));
        eventPublisher.publishEvent(new LateFeeRegisteredEvent(
                fee.getId(),
                loan.getId(),
                loan.getBorrower().getUsername(),
                calculation.daysLate(),
                calculation.amount(),
                calculation.currency(),
                returnedAt
        ));
        return LateFeeResponse.from(fee);
    }
}
