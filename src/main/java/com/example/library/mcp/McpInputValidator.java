package com.example.library.mcp;

import java.util.Comparator;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
class McpInputValidator {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final Validator validator;

    McpInputValidator(Validator validator) {
        this.validator = validator;
    }

    void validate(Object input) {
        var violations = validator.validate(input);
        if (!violations.isEmpty()) {
            String details = violations.stream()
                    .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                    .map(McpInputValidator::message)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Invalid MCP tool input: " + details);
        }
    }

    long positiveId(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be a positive integer");
        }
        return value;
    }

    PageRequest page(Integer page, Integer size, Sort sort) {
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? DEFAULT_PAGE_SIZE : size;
        if (resolvedPage < 0) {
            throw new IllegalArgumentException("page must be zero or greater");
        }
        if (resolvedSize < 1 || resolvedSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        return PageRequest.of(resolvedPage, resolvedSize, sort);
    }

    private static String message(ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + " " + violation.getMessage();
    }
}
