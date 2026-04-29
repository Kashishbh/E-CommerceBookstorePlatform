package com.booknest.catalog.service.impl;

import com.booknest.catalog.dto.BookRequest;
import com.booknest.catalog.dto.BookResponse;
import com.booknest.catalog.producer.NotificationEventProducer;
import com.booknest.catalog.entity.Book;
import com.booknest.catalog.exception.BadRequestException;
import com.booknest.catalog.exception.ResourceNotFoundException;
import com.booknest.catalog.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private NotificationEventProducer notificationEventProducer;

    @InjectMocks
    private BookServiceImpl bookService;

    @Test
    void addBook_shouldSaveBookSuccessfully() {
        BookRequest request = buildBookRequest();
        Book savedBook = buildBook();
        savedBook.setBookId(1L);

        when(bookRepository.findByIsbn("ISBN-123")).thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenReturn(savedBook);

        BookResponse response = bookService.addBook(request);

        assertNotNull(response);
        assertEquals(1L, response.getBookId());
        assertEquals("Atomic Habits", response.getTitle());
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void addBook_shouldThrowExceptionWhenIsbnExists() {
        BookRequest request = buildBookRequest();

        when(bookRepository.findByIsbn("ISBN-123")).thenReturn(Optional.of(buildBook()));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> bookService.addBook(request)
        );

        assertEquals("Book with this ISBN already exists", exception.getMessage());
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void getBookById_shouldReturnBookSuccessfully() {
        Book book = buildBook();
        book.setBookId(1L);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        BookResponse response = bookService.getBookById(1L);

        assertEquals(1L, response.getBookId());
        assertEquals("Atomic Habits", response.getTitle());
        assertEquals("James Clear", response.getAuthor());
    }

    @Test
    void getBookById_shouldThrowExceptionWhenNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookService.getBookById(99L));
    }

    @Test
    void searchBooks_shouldReturnMatchingBooks() {
        Book book = buildBook();
        book.setBookId(1L);

        when(bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrGenreContainingIgnoreCase(
                "Atomic", "Atomic", "Atomic"
        )).thenReturn(List.of(book));

        List<BookResponse> responses = bookService.searchBooks("Atomic");

        assertEquals(1, responses.size());
        assertEquals("Atomic Habits", responses.get(0).getTitle());
    }

    @Test
    void getByGenre_shouldReturnGenreBooks() {
        Book book = buildBook();
        book.setBookId(1L);

        when(bookRepository.findByGenreIgnoreCase("Self Help")).thenReturn(List.of(book));

        List<BookResponse> responses = bookService.getByGenre("Self Help");

        assertEquals(1, responses.size());
        assertEquals("Self Help", responses.get(0).getGenre());
    }

    @Test
    void updateBook_shouldUpdateSuccessfully() {
        Book existing = buildBook();
        existing.setBookId(1L);

        BookRequest request = buildBookRequest();
        request.setTitle("Updated Title");
        request.setPrice(new BigDecimal("599"));

        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse response = bookService.updateBook(1L, request);

        assertEquals("Updated Title", response.getTitle());
        assertEquals(new BigDecimal("599"), response.getPrice());
    }

    @Test
    void deleteBook_shouldDeleteSuccessfully() {
        Book existing = buildBook();
        existing.setBookId(1L);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));

        bookService.deleteBook(1L);

        verify(bookRepository).delete(existing);
    }

    @Test
    void updateStock_shouldUpdateSuccessfully() {
        Book existing = buildBook();
        existing.setBookId(1L);
        existing.setStock(10);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse response = bookService.updateStock(1L, 7);

        assertEquals(7, response.getStock());
    }

    @Test
    void updateStock_shouldThrowExceptionWhenStockNegative() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> bookService.updateStock(1L, -1)
        );

        assertEquals("Stock cannot be negative", exception.getMessage());
    }

    @Test
    void updateStock_shouldPublishLowStockAlertWhenThresholdReached() {
        Book existing = buildBook();
        existing.setBookId(1L);
        existing.setStock(10);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse response = bookService.updateStock(1L, 4);

        assertEquals(4, response.getStock());
        verify(notificationEventProducer).publish(any());
    }

    @Test
    void getFeaturedBooks_shouldReturnFeaturedOnly() {
        Book featuredBook = buildBook();
        featuredBook.setBookId(1L);
        featuredBook.setFeatured(true);

        when(bookRepository.findByFeaturedTrue()).thenReturn(List.of(featuredBook));

        List<BookResponse> responses = bookService.getFeaturedBooks();

        assertEquals(1, responses.size());
        assertTrue(responses.get(0).getFeatured());
    }

    @Test
    void addBook_shouldThrowExceptionWhenPriceInvalid() {
        BookRequest request = buildBookRequest();
        request.setPrice(BigDecimal.ZERO);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> bookService.addBook(request)
        );

        assertEquals("Price must be greater than 0", exception.getMessage());
    }

    @Test
    void addBook_shouldThrowExceptionWhenStockNegative() {
        BookRequest request = buildBookRequest();
        request.setStock(-1);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> bookService.addBook(request)
        );

        assertEquals("Stock cannot be negative", exception.getMessage());
    }

    @Test
    void updateStock_shouldCaptureSavedBookWithUpdatedStock() {
        Book existing = buildBook();
        existing.setBookId(1L);
        existing.setStock(10);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        bookService.updateStock(1L, 3);

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());

        Book savedBook = captor.getValue();
        assertEquals(3, savedBook.getStock());
    }

    private BookRequest buildBookRequest() {
        BookRequest request = new BookRequest();
        request.setTitle("Atomic Habits");
        request.setAuthor("James Clear");
        request.setIsbn("ISBN-123");
        request.setGenre("Self Help");
        request.setPublisher("Random House");
        request.setPrice(new BigDecimal("499"));
        request.setStock(10);
        request.setRating(4.8);
        request.setDescription("Good book");
        request.setCoverImageUrl("cover.jpg");
        request.setPublishedDate(LocalDate.of(2020, 1, 1));
        request.setFeatured(true);
        return request;
    }

    private Book buildBook() {
        Book book = new Book();
        book.setBookId(1L);
        book.setTitle("Atomic Habits");
        book.setAuthor("James Clear");
        book.setIsbn("ISBN-123");
        book.setGenre("Self Help");
        book.setPublisher("Random House");
        book.setPrice(new BigDecimal("499"));
        book.setStock(10);
        book.setRating(4.8);
        book.setDescription("Good book");
        book.setCoverImageUrl("cover.jpg");
        book.setPublishedDate(LocalDate.of(2020, 1, 1));
        book.setFeatured(true);
        return book;
    }
}
