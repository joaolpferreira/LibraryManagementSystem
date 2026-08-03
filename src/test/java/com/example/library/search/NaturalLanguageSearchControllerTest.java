package com.example.library.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NaturalLanguageSearchControllerTest {

    @Mock
    private NaturalLanguageSearchService service;

    @Test
    void delegatesNaturalLanguageSearch() {
        NaturalLanguageSearchController controller = new NaturalLanguageSearchController(service);
        PageRequest pageable = PageRequest.of(0, 10);
        NaturalLanguageSearchResponse expected = mock(NaturalLanguageSearchResponse.class);
        when(service.search("available books", pageable)).thenReturn(expected);

        assertThat(controller.search("available books", pageable)).isSameAs(expected);
    }
}
