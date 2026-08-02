package com.example.library.metadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookMetadataControllerTest {

    @Mock
    private BookMetadataService service;

    @Test
    void delegatesReadAndEnrichment() {
        BookMetadataController controller = new BookMetadataController(service);
        BookMetadataResponse response = mock(BookMetadataResponse.class);
        when(service.get(1L)).thenReturn(response);
        when(service.enrich(1L)).thenReturn(response);

        assertThat(controller.get(1L)).isSameAs(response);
        assertThat(controller.enrich(1L)).isSameAs(response);
    }
}
