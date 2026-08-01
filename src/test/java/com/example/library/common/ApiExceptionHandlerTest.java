package com.example.library.common;

import java.util.List;
import java.util.Map;

import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void mapsNotFoundConflictForbiddenAndConstraintErrors() {
        assertProblem(
                handler.handleNotFound(new ResourceNotFoundException("missing")),
                HttpStatus.NOT_FOUND,
                "Resource not found",
                "missing"
        );
        assertProblem(
                handler.handleConflict(new ConflictException("duplicate")),
                HttpStatus.CONFLICT,
                "Request conflicts with current state",
                "duplicate"
        );
        assertProblem(
                handler.handleAccessDenied(new AccessDeniedException("denied")),
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "denied"
        );
        assertProblem(
                handler.handleConstraintViolation(new ConstraintViolationException("invalid", null)),
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "invalid"
        );
    }

    @Test
    void validationCollectsTheFirstMessageForEachField() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("bookRequest", "title", "must not be blank"),
                new FieldError("bookRequest", "title", "must be shorter"),
                new FieldError("bookRequest", "isbn", "must be valid")
        ));

        ProblemDetail problem = handler.handleValidation(exception);

        assertProblem(problem, HttpStatus.BAD_REQUEST, "Validation failed", "One or more request fields are invalid");
        assertThat(problem.getProperties()).containsEntry(
                "fieldErrors",
                Map.of("title", "must not be blank", "isbn", "must be valid")
        );
    }

    @Test
    void dataIntegrityErrorsDoNotLeakDatabaseDetails() {
        ProblemDetail problem = handler.handleDataIntegrity(
                new DataIntegrityViolationException("secret database message")
        );

        assertProblem(
                problem,
                HttpStatus.CONFLICT,
                "Database constraint violation",
                "The operation conflicts with existing data"
        );
    }

    private static void assertProblem(
            ProblemDetail problem,
            HttpStatus status,
            String title,
            String detail
    ) {
        assertThat(problem.getStatus()).isEqualTo(status.value());
        assertThat(problem.getTitle()).isEqualTo(title);
        assertThat(problem.getDetail()).isEqualTo(detail);
        assertThat(problem.getType()).hasToString("about:blank");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }
}
