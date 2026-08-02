package com.example.library.assistant;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantIntentClassifierTest {

    private final AssistantIntentClassifier classifier = new AssistantIntentClassifier();

    @ParameterizedTest
    @CsvSource({
            "Ajuda, HELP",
            "Recomenda-me alguma coisa, RECOMMENDATIONS",
            "Tenho alguma multa?, MY_LATE_FEES",
            "Do I have a fine?, MY_LATE_FEES",
            "Mostra a minha fila de reservas, MY_RESERVATIONS",
            "Quais são os meus empréstimos?, MY_LOANS",
            "livros disponíveis de Martin Fowler, SEARCH_BOOKS"
    })
    void classifiesEnglishAndPortugueseMessages(String message, AssistantIntent expected) {
        assertThat(classifier.classify(message)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "coffee books, SEARCH_BOOKS",
            "A reservationless architecture book, SEARCH_BOOKS",
            "fine-grained concurrency, SEARCH_BOOKS"
    })
    void doesNotClassifyKeywordsEmbeddedInsideOtherWords(
            String message,
            AssistantIntent expected
    ) {
        assertThat(classifier.classify(message)).isEqualTo(expected);
    }
}
