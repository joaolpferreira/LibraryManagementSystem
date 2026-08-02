package com.example.library.metadata;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetadataSnapshotTest {

    @Test
    void sanitizesUntrustedProviderValuesToPersistenceLimits() {
        List<String> subjects = new ArrayList<>();
        subjects.add(null);
        subjects.add("  ");
        subjects.add(" Software   Design ");
        subjects.add("software design");
        for (int index = 0; index < 25; index++) {
            subjects.add("Subject " + index + " ".repeat(220));
        }
        String longPublisher = "📚".repeat(250);
        String longUrl = "https://example.test/" + "x".repeat(600);

        MetadataSnapshot snapshot = new MetadataSnapshot(
                "  " + longPublisher + "  ",
                999,
                subjects,
                longUrl,
                " OPEN   LIBRARY ",
                longUrl
        );

        assertThat(snapshot.publisher().codePointCount(0, snapshot.publisher().length()))
                .isEqualTo(200);
        assertThat(snapshot.publishedYear()).isNull();
        assertThat(snapshot.subjects()).hasSize(20);
        assertThat(snapshot.subjects().getFirst()).isEqualTo("Software Design");
        assertThat(snapshot.subjects()).allSatisfy(subject ->
                assertThat(subject.codePointCount(0, subject.length())).isLessThanOrEqualTo(200));
        assertThat(snapshot.coverUrl()).hasSize(500);
        assertThat(snapshot.source()).isEqualTo("OPEN LIBRARY");
        assertThat(snapshot.sourceUrl()).hasSize(500);
        assertThat(snapshot.subjects()).isUnmodifiable();
    }

    @Test
    void preservesValidValuesAndNormalizesMissingOptionalData() {
        MetadataSnapshot snapshot = new MetadataSnapshot(
                "Publisher", 2026, null, " ", "TEST", null
        );
        MetadataSnapshot implausibleFuture = new MetadataSnapshot(
                null, 10_000, List.of(), null, "TEST", null
        );

        assertThat(snapshot.publisher()).isEqualTo("Publisher");
        assertThat(snapshot.publishedYear()).isEqualTo(2026);
        assertThat(snapshot.subjects()).isEmpty();
        assertThat(snapshot.coverUrl()).isNull();
        assertThat(snapshot.sourceUrl()).isNull();
        assertThat(implausibleFuture.publishedYear()).isNull();
    }

    @Test
    void requiresAProviderSource() {
        assertThatThrownBy(() -> new MetadataSnapshot(null, null, List.of(), null, " ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Metadata source is required");
    }
}
