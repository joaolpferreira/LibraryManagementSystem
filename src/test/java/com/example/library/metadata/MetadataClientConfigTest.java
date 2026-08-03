package com.example.library.metadata;

import java.time.Duration;

import com.example.library.config.MetadataProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetadataClientConfigTest {

    @Test
    void createsATimeBoundIdentifiedRestClient() {
        MetadataProperties properties = new MetadataProperties(
                "https://openlibrary.org",
                "https://covers.openlibrary.org",
                Duration.ofMillis(100),
                Duration.ofMillis(200),
                "test-agent"
        );

        assertThat(new MetadataClientConfig().openLibraryRestClient(properties)).isNotNull();
    }
}
