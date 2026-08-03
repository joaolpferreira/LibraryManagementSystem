package com.example.library.metadata;

import java.util.List;
import java.util.Optional;

import com.example.library.common.ExternalServiceException;
import com.example.library.config.MetadataProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenLibraryMetadataProvider implements BookMetadataProvider {

    private final RestClient restClient;
    private final String baseUrl;
    private final String coversBaseUrl;

    public OpenLibraryMetadataProvider(
            RestClient openLibraryRestClient,
            MetadataProperties properties
    ) {
        restClient = openLibraryRestClient;
        baseUrl = properties.baseUrl();
        coversBaseUrl = properties.coversBaseUrl();
    }

    @Override
    public Optional<MetadataSnapshot> findByIsbn(String isbn) {
        try {
            SearchResponse response = restClient.get()
                    .uri(uri -> uri.path("/search.json")
                            .queryParam("isbn", isbn)
                            .queryParam(
                                    "fields",
                                    "key,publisher,first_publish_year,subject,cover_i"
                            )
                            .queryParam("limit", 1)
                            .build())
                    .retrieve()
                    .body(SearchResponse.class);
            return firstDocument(response).map(this::toSnapshot);
        } catch (RestClientException exception) {
            throw new ExternalServiceException(
                    "Open Library could not be reached; retry metadata enrichment later",
                    exception
            );
        }
    }

    private Optional<SearchDocument> firstDocument(SearchResponse response) {
        if (response == null || response.documents() == null || response.documents().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(response.documents().getFirst());
    }

    private MetadataSnapshot toSnapshot(SearchDocument document) {
        String publisher = firstOrNull(document.publishers());
        List<String> subjects = document.subjects() == null ? List.of() : document.subjects();
        String coverUrl = document.coverId() == null
                ? null
                : coversBaseUrl + "/b/id/" + document.coverId() + "-L.jpg?default=false";
        String sourceUrl = document.key() == null ? null : baseUrl + document.key();
        return new MetadataSnapshot(
                publisher,
                document.firstPublishedYear(),
                subjects,
                coverUrl,
                "OPEN_LIBRARY",
                sourceUrl
        );
    }

    private static String firstOrNull(List<String> values) {
        return values == null || values.isEmpty() ? null : values.getFirst();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SearchResponse(@JsonProperty("docs") List<SearchDocument> documents) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SearchDocument(
            String key,
            @JsonProperty("publisher") List<String> publishers,
            @JsonProperty("first_publish_year") Integer firstPublishedYear,
            @JsonProperty("subject") List<String> subjects,
            @JsonProperty("cover_i") Long coverId
    ) {
    }
}
