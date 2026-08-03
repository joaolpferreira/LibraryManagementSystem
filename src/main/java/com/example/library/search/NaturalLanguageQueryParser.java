package com.example.library.search;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class NaturalLanguageQueryParser {

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final List<Pattern> UNAVAILABLE_PATTERNS = List.of(
            Pattern.compile(
                    "\\b(not\\s+(?:currently\\s+)?available|unavailable|checked\\s+out|"
                            + "out\\s+of\\s+stock)\\b",
                    FLAGS
            ),
            Pattern.compile(
                    "\\b(sem\\s+disponibilidade|indispon[ií]ve(?:l|is)|esgotad[oa]s?|"
                            + "emprestad[oa]s?)\\b",
                    FLAGS
            )
    );
    private static final Pattern AVAILABLE = Pattern.compile(
            "\\b((?:currently\\s+)?available|in\\s+stock|dispon[ií]ve(?:l|is)|em\\s+stock)\\b",
            FLAGS
    );
    private static final List<Pattern> FILLER_PATTERNS = List.of(
            Pattern.compile(
                    "\\b(show\\s+me|find|search\\s+for|that\\s+are)\\b",
                    FLAGS
            ),
            Pattern.compile(
                    "\\b(books?|written\\s+by|by|about|only)\\b",
                    FLAGS
            ),
            Pattern.compile(
                    "\\b(que\\s+est[aã]o|quero|procura(?:r)?|mostra(?:-me)?)\\b",
                    FLAGS
            ),
            Pattern.compile(
                    "\\b(livros?|do\\s+autor|da\\s+autora|por|sobre|apenas|somente)\\b",
                    FLAGS
            )
    );
    private static final Pattern PUNCTUATION = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern SPACE = Pattern.compile("\\s+");

    public NaturalLanguageQuery parse(String question) {
        String normalized = question.trim().toLowerCase(Locale.ROOT);
        Boolean availability = null;
        if (matchesAny(UNAVAILABLE_PATTERNS, normalized)) {
            availability = false;
            normalized = removeMatches(UNAVAILABLE_PATTERNS, normalized);
        } else if (AVAILABLE.matcher(normalized).find()) {
            availability = true;
            normalized = AVAILABLE.matcher(normalized).replaceAll(" ");
        }
        String catalogQuery = SPACE.matcher(PUNCTUATION.matcher(
                        removeMatches(FILLER_PATTERNS, normalized)
                ).replaceAll(" "))
                .replaceAll(" ")
                .trim();
        return new NaturalLanguageQuery(question.trim(), catalogQuery, availability);
    }

    private static boolean matchesAny(List<Pattern> patterns, String value) {
        return patterns.stream().anyMatch(pattern -> pattern.matcher(value).find());
    }

    private static String removeMatches(List<Pattern> patterns, String value) {
        String result = value;
        for (Pattern pattern : patterns) {
            result = pattern.matcher(result).replaceAll(" ");
        }
        return result;
    }
}
