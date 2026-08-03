package com.example.library.config;

import java.time.Duration;
import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "library.metadata")
public record MetadataProperties(
        String baseUrl,
        String coversBaseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        String userAgent
) {
    public MetadataProperties {
        baseUrl = normalizeBaseUrl(defaultIfBlank(baseUrl, "https://openlibrary.org"));
        coversBaseUrl = normalizeBaseUrl(defaultIfBlank(
                coversBaseUrl,
                "https://covers.openlibrary.org"
        ));
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(5) : readTimeout;
        userAgent = defaultIfBlank(
                userAgent,
                "LibraryManagementSystem/1.0 (metadata enrichment)"
        );
        if (!connectTimeout.isPositive() || !readTimeout.isPositive()) {
            throw new IllegalArgumentException("Metadata HTTP timeouts must be positive");
        }
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String normalizeBaseUrl(String value) {
        String normalized = StringUtils.trimTrailingCharacter(value.trim(), '/');
        try {
            URI uri = URI.create(normalized);
            String scheme = uri.getScheme();
            if (uri.getHost() == null
                    || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException();
            }
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Metadata provider URLs must be absolute HTTP or HTTPS URLs",
                    exception
            );
        }
    }
}
