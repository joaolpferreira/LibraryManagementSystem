package com.example.library.metadata;

import java.time.Duration;

import com.example.library.common.ExternalServiceException;
import com.example.library.config.MetadataProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenLibraryMetadataProviderTest {

    private MockRestServiceServer server;
    private OpenLibraryMetadataProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        MetadataProperties properties = new MetadataProperties(
                "https://catalog.test",
                "https://covers.test",
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                "test-agent"
        );
        provider = new OpenLibraryMetadataProvider(builder.build(), properties);
    }

    @Test
    void mapsAndSanitizesTheBestOpenLibraryDocument() {
        expectSearch(withSuccess("""
                {
                  "docs": [{
                    "key": "/works/OL1W",
                    "publisher": ["Prentice Hall", "Other"],
                    "first_publish_year": 2008,
                    "subject": [" Programming ", null, "", "Programming", "Design"],
                    "cover_i": 123
                  }]
                }
                """, MediaType.APPLICATION_JSON));

        MetadataSnapshot result = provider.findByIsbn("9780132350884").orElseThrow();

        assertThat(result.publisher()).isEqualTo("Prentice Hall");
        assertThat(result.publishedYear()).isEqualTo(2008);
        assertThat(result.subjects()).containsExactly("Programming", "Design");
        assertThat(result.coverUrl()).isEqualTo(
                "https://covers.test/b/id/123-L.jpg?default=false"
        );
        assertThat(result.source()).isEqualTo("OPEN_LIBRARY");
        assertThat(result.sourceUrl()).isEqualTo("https://catalog.test/works/OL1W");
        server.verify();
    }

    @Test
    void supportsSparseDocumentsAndEmptySearchResults() {
        expectSearch(withSuccess("{\"docs\":[{}]}", MediaType.APPLICATION_JSON));
        expectSearch(withSuccess("{\"docs\":[]}", MediaType.APPLICATION_JSON));
        expectSearch(withSuccess("{}", MediaType.APPLICATION_JSON));
        expectSearch(withSuccess());
        expectSearch(withSuccess("{\"docs\":[null]}", MediaType.APPLICATION_JSON));

        MetadataSnapshot sparse = provider.findByIsbn("one").orElseThrow();
        assertThat(sparse.publisher()).isNull();
        assertThat(sparse.subjects()).isEmpty();
        assertThat(sparse.coverUrl()).isNull();
        assertThat(sparse.sourceUrl()).isNull();
        assertThat(provider.findByIsbn("two")).isEmpty();
        assertThat(provider.findByIsbn("three")).isEmpty();
        assertThat(provider.findByIsbn("four")).isEmpty();
        assertThat(provider.findByIsbn("five")).isEmpty();
        server.verify();
    }

    @Test
    void treatsAnEmptyPublisherListAsMissing() {
        expectSearch(withSuccess("{\"docs\":[{\"publisher\":[]}]}", MediaType.APPLICATION_JSON));

        assertThat(provider.findByIsbn("9780132350884").orElseThrow().publisher()).isNull();
    }

    @Test
    void translatesRemoteFailuresWithoutLeakingClientDetails() {
        expectSearch(withServerError());

        assertThatThrownBy(() -> provider.findByIsbn("9780132350884"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("retry metadata enrichment later")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    private void expectSearch(org.springframework.test.web.client.ResponseCreator response) {
        server.expect(once(), requestTo(org.hamcrest.Matchers.containsString("/search.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(response);
    }
}
