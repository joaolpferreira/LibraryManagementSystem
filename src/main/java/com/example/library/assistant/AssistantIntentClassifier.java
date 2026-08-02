package com.example.library.assistant;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class AssistantIntentClassifier {

    private static final Pattern HELP = Pattern.compile(
            "\\b(help|ajuda|capabilities|opcoes)\\b"
    );
    private static final Pattern RECOMMENDATIONS = Pattern.compile(
            "\\b(recommend\\w*|suger\\w*|recomend\\w*)\\b"
    );
    private static final Pattern LATE_FEES = Pattern.compile(
            "\\b(late\\s+fees?|fines?(?!\\s+grained\\b)|fees?|multas?)\\b"
    );
    private static final Pattern RESERVATIONS = Pattern.compile(
            "\\b(reservations?|reserve|queues?|reservas?|fila)\\b"
    );
    private static final Pattern LOANS = Pattern.compile(
            "\\b(my\\s+loans?|borrowed|loans?|emprest\\w*)\\b"
    );

    public AssistantIntent classify(String message) {
        String text = normalize(message);
        if (HELP.matcher(text).find()) {
            return AssistantIntent.HELP;
        }
        if (RECOMMENDATIONS.matcher(text).find()) {
            return AssistantIntent.RECOMMENDATIONS;
        }
        if (LATE_FEES.matcher(text).find()) {
            return AssistantIntent.MY_LATE_FEES;
        }
        if (RESERVATIONS.matcher(text).find()) {
            return AssistantIntent.MY_RESERVATIONS;
        }
        if (LOANS.matcher(text).find()) {
            return AssistantIntent.MY_LOANS;
        }
        return AssistantIntent.SEARCH_BOOKS;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }
}
