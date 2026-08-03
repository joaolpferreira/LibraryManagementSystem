package com.example.library.search;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NaturalLanguageQueryParserTest {

    private final NaturalLanguageQueryParser parser = new NaturalLanguageQueryParser();

    @Test
    void detectsUnavailableEnglishIntentBeforeTheAvailableSubstring() {
        NaturalLanguageQuery query = parser.parse(" Show me books that are NOT available! ");

        assertThat(query.originalQuestion()).isEqualTo("Show me books that are NOT available!");
        assertThat(query.catalogQuery()).isEmpty();
        assertThat(query.availableOnly()).isFalse();
    }

    @Test
    void detectsPortugueseAvailableIntentAndRemovesFillerWords() {
        NaturalLanguageQuery query = parser.parse("Quero livros disponíveis por Martin Fowler");

        assertThat(query.catalogQuery()).isEqualTo("martin fowler");
        assertThat(query.availableOnly()).isTrue();
    }

    @Test
    void preservesAccentsInSearchableNamesAndUnderstandsMoreAvailabilityPhrases() {
        NaturalLanguageQuery portuguese = parser.parse(
                "Mostra-me apenas livros em stock por João Tordo"
        );
        NaturalLanguageQuery english = parser.parse(
                "books about distributed systems that are out of stock"
        );

        assertThat(portuguese.catalogQuery()).isEqualTo("joão tordo");
        assertThat(portuguese.availableOnly()).isTrue();
        assertThat(english.catalogQuery()).isEqualTo("distributed systems");
        assertThat(english.availableOnly()).isFalse();
    }

    @Test
    void recognizesCurrentlyAvailableAndPortugueseUnavailableVariants() {
        assertThat(parser.parse("currently available books").availableOnly()).isTrue();
        assertThat(parser.parse("livros esgotados").availableOnly()).isFalse();
        assertThat(parser.parse("livro indisponível").availableOnly()).isFalse();
    }

    @Test
    void keepsAPlainCatalogQuestionUnfiltered() {
        NaturalLanguageQuery query = parser.parse("Domain-Driven Design");

        assertThat(query.catalogQuery()).isEqualTo("domain driven design");
        assertThat(query.availableOnly()).isNull();
    }
}
