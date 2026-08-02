package com.example.library.search;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class NaturalLanguageQueryParser {

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final Pattern UNAVAILABLE = Pattern.compile(
            "\\b(not\\s+(?:currently\\s+)?available|unavailable|checked\\s+out|out\\s+of\\s+stock|"
                    + "sem\\s+disponibilidade|indispon[ií]ve(?:l|is)|esgotad[oa]s?|"
                    + "emprestad[oa]s?)\\b",
            FLAGS
    );
    private static final Pattern AVAILABLE = Pattern.compile(
            "\\b((?:currently\\s+)?available|in\\s+stock|dispon[ií]ve(?:l|is)|em\\s+stock)\\b",
            FLAGS
    );
    private static final Pattern FILLER = Pattern.compile(
            "\\b(show\\s+me|find|search\\s+for|that\\s+are|que\\s+est[aã]o|books?|livros?|"
                    + "quero|procura(?:r)?|mostra(?:-me)?|written\\s+by|by|do\\s+autor|"
                    + "da\\s+autora|por|about|sobre|only|apenas|somente)\\b",
            FLAGS
    );
    private static final Pattern PUNCTUATION = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern SPACE = Pattern.compile("\\s+");

    public NaturalLanguageQuery parse(String question) {
        String normalized = question.trim().toLowerCase(Locale.ROOT);
        Boolean availability = null;
        if (UNAVAILABLE.matcher(normalized).find()) {
            availability = false;
            normalized = UNAVAILABLE.matcher(normalized).replaceAll(" ");
        } else if (AVAILABLE.matcher(normalized).find()) {
            availability = true;
            normalized = AVAILABLE.matcher(normalized).replaceAll(" ");
        }
        String catalogQuery = SPACE.matcher(PUNCTUATION.matcher(
                        FILLER.matcher(normalized).replaceAll(" ")
                ).replaceAll(" "))
                .replaceAll(" ")
                .trim();
        return new NaturalLanguageQuery(question.trim(), catalogQuery, availability);
    }
}
