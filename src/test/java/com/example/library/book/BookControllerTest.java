package com.example.library.book;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookControllerTest {

    @Mock
    private BookService service;

    private BookController controller;

    @BeforeEach
    void setUp() {
        controller = new BookController(service);
    }

    @Test
    void delegatesSearchGetUpdateAndRemove() {
        PageRequest pageable = PageRequest.of(0, 10);
        BookRequest request = new BookRequest("9780132350884", "Clean Code", "Robert C. Martin", null, 1);
        BookResponse response = new BookResponse(1L, request.isbn(), request.title(), request.author(), null, 1, 1, true);
        when(service.search("clean", true, pageable)).thenReturn(Page.empty());
        when(service.get(1L)).thenReturn(response);
        when(service.update(1L, request)).thenReturn(response);

        assertThat(controller.search("clean", true, pageable)).isEmpty();
        assertThat(controller.get(1L)).isEqualTo(response);
        assertThat(controller.update(1L, request)).isEqualTo(response);
        assertThat(controller.remove(1L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(service).remove(1L);
    }
}
