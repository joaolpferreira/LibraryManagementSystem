package com.example.library.config;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetadataPropertiesTest {

    @Test
    void suppliesSafeDefaultsForMissingAndBlankValues() {
        MetadataProperties missing = new MetadataProperties(null, null, null, null, null);
        MetadataProperties blank = new MetadataProperties(" ", " ", null, null, " ");

        assertThat(missing.baseUrl()).isEqualTo("https://openlibrary.org");
        assertThat(blank.coversBaseUrl()).isEqualTo("https://covers.openlibrary.org");
        assertThat(missing.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(missing.readTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(blank.userAgent()).contains("LibraryManagementSystem");
    }

    @Test
    void preservesExplicitConfiguration() {
        MetadataProperties properties = new MetadataProperties(
                "https://catalog.test",
                "https://covers.test",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                "test-agent"
        );

        assertThat(properties.baseUrl()).isEqualTo("https://catalog.test");
        assertThat(properties.coversBaseUrl()).isEqualTo("https://covers.test");
        assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(1));
        assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(properties.userAgent()).isEqualTo("test-agent");
    }

    @Test
    void normalizesProviderUrlsAndRejectsUnsafeConfiguration() {
        MetadataProperties properties = new MetadataProperties(
                "https://catalog.test///",
                "http://localhost:8081/",
                Duration.ofMillis(1),
                Duration.ofMillis(1),
                "agent"
        );
        assertThat(properties.baseUrl()).isEqualTo("https://catalog.test");
        assertThat(properties.coversBaseUrl()).isEqualTo("http://localhost:8081");

        assertThatThrownBy(() -> new MetadataProperties(
                "file:///tmp/catalog", "https://covers.test",
                Duration.ofSeconds(1), Duration.ofSeconds(1), "agent"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("absolute HTTP or HTTPS");
        assertThatThrownBy(() -> new MetadataProperties(
                "ftp://catalog.test", "https://covers.test",
                Duration.ofSeconds(1), Duration.ofSeconds(1), "agent"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("absolute HTTP or HTTPS");
        assertThatThrownBy(() -> new MetadataProperties(
                "not a url", "https://covers.test",
                Duration.ofSeconds(1), Duration.ofSeconds(1), "agent"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("absolute HTTP or HTTPS");
        assertThatThrownBy(() -> new MetadataProperties(
                "https://catalog.test", "https://covers.test",
                Duration.ZERO, Duration.ofSeconds(1), "agent"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeouts must be positive");
        assertThatThrownBy(() -> new MetadataProperties(
                "https://catalog.test", "https://covers.test",
                Duration.ofSeconds(1), Duration.ofSeconds(-1), "agent"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeouts must be positive");
    }
}
