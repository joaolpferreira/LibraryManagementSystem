package com.example.library.book;

import java.util.Optional;

import com.example.library.common.ConflictException;
import com.example.library.common.ResourceNotFoundException;
import com.example.library.reservation.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private ReservationService reservationService;

    private BookService service;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        service = new BookService(bookRepository, reservationService);
        pageable = PageRequest.of(0, 20);
    }

    @Test
    void searchNormalizesEveryQueryAndAvailabilityCombination() {
        Book book = book("Description", 1);
        when(bookRepository.search(any(), any(Integer.class), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(book)));

        Page<BookResponse> allWithNullQuery = service.search(null, null, pageable);
        service.search("   ", true, pageable);
        service.search("  Clean  ", false, pageable);

        assertThat(allWithNullQuery.getContent()).hasSize(1);
        verify(bookRepository).search("", -1, pageable);
        verify(bookRepository).search("", 1, pageable);
        verify(bookRepository).search("Clean", 0, pageable);
    }

    @Test
    void getReturnsAnActiveBookOrNotFound() {
        Book book = book(null, 1);
        when(bookRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(book));
        when(bookRepository.findByIdAndActiveTrue(2L)).thenReturn(Optional.empty());

        assertThat(service.get(1L).title()).isEqualTo("Clean Code");
        assertThatThrownBy(() -> service.get(2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Book 2 was not found");
    }

    @Test
    void createNormalizesInputAndSupportsNullBlankAndPresentDescriptions() {
        when(bookRepository.existsByIsbnIgnoreCase(any())).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse withNull = service.create(request("978-0-13-235088-4", null, 1));
        BookResponse withBlank = service.create(request("978 0 13 235088 4", "   ", 1));
        BookResponse withText = service.create(request("9780132350884", "  A classic  ", 1));

        assertThat(withNull.isbn()).isEqualTo("9780132350884");
        assertThat(withNull.description()).isNull();
        assertThat(withBlank.description()).isNull();
        assertThat(withText.description()).isEqualTo("A classic");
    }

    @Test
    void createRejectsAnExistingIsbn() {
        BookRequest request = request("978-0-13-235088-4", null, 1);
        when(bookRepository.existsByIsbnIgnoreCase("9780132350884")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void updateHandlesSuccessMissingDuplicateAndBorrowedInventoryConflicts() {
        Book success = book(null, 1);
        when(bookRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(success));
        when(bookRepository.existsByIsbnIgnoreCaseAndIdNot("9780132350884", 1L)).thenReturn(false);

        BookResponse updated = service.update(1L, request("978-0-13-235088-4", " Updated ", 2));
        assertThat(updated.description()).isEqualTo("Updated");
        assertThat(updated.totalCopies()).isEqualTo(2);

        when(bookRepository.findActiveByIdForUpdate(2L)).thenReturn(Optional.empty());
        BookRequest missingRequest = request("9780132350884", null, 1);
        assertThatThrownBy(() -> service.update(2L, missingRequest))
                .isInstanceOf(ResourceNotFoundException.class);

        Book duplicate = book(null, 1);
        when(bookRepository.findActiveByIdForUpdate(3L)).thenReturn(Optional.of(duplicate));
        when(bookRepository.existsByIsbnIgnoreCaseAndIdNot("9780132350884", 3L)).thenReturn(true);
        BookRequest duplicateRequest = request("9780132350884", null, 1);
        assertThatThrownBy(() -> service.update(3L, duplicateRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        Book borrowed = book(null, 2);
        borrowed.borrowCopy();
        borrowed.borrowCopy();
        when(bookRepository.findActiveByIdForUpdate(4L)).thenReturn(Optional.of(borrowed));
        when(bookRepository.existsByIsbnIgnoreCaseAndIdNot("9780132350884", 4L)).thenReturn(false);
        BookRequest borrowedRequest = request("9780132350884", null, 1);
        assertThatThrownBy(() -> service.update(4L, borrowedRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("currently on loan");
    }

    @Test
    void removeHandlesSuccessMissingAndActiveLoanConflict() {
        Book removable = book(null, 1);
        when(bookRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(removable));
        service.remove(1L);
        assertThat(removable.isActive()).isFalse();

        when(bookRepository.findActiveByIdForUpdate(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.remove(2L))
                .isInstanceOf(ResourceNotFoundException.class);

        Book borrowed = book(null, 1);
        borrowed.borrowCopy();
        when(bookRepository.findActiveByIdForUpdate(3L)).thenReturn(Optional.of(borrowed));
        assertThatThrownBy(() -> service.remove(3L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("active loans");
    }

    private static Book book(String description, int copies) {
        return new Book("9780132350884", "Clean Code", "Robert C. Martin", description, copies);
    }

    private static BookRequest request(String isbn, String description, int copies) {
        return new BookRequest(isbn, " Clean Code ", " Robert C. Martin ", description, copies);
    }
}
