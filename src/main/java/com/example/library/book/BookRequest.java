package com.example.library.book;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank
        @Size(max = 20)
        @Pattern(
                regexp = "(?i)(?:\\d[ -]?){9}[\\dX]|(?:\\d[ -]?){12}\\d",
                message = "must be a valid ISBN-10 or ISBN-13"
        )
        String isbn,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 150) String author,
        @Size(max = 2_000) String description,
        @Min(1) @Max(10_000) int totalCopies
) {
}
