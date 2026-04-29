package com.booknest.catalog.service.impl;

import com.booknest.catalog.dto.BookRequest;
import com.booknest.catalog.dto.BookResponse;
import com.booknest.catalog.dto.NotificationEvent;
import com.booknest.catalog.entity.Book;
import com.booknest.catalog.exception.BadRequestException;
import com.booknest.catalog.exception.ResourceNotFoundException;
import com.booknest.catalog.producer.NotificationEventProducer;
import com.booknest.catalog.repository.BookRepository;
import com.booknest.catalog.service.BookService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookServiceImpl implements BookService {

    private static final int LOW_STOCK_THRESHOLD = 5L > Integer.MAX_VALUE ? 5 : 5;

    private final BookRepository bookRepository;
    private final NotificationEventProducer notificationEventProducer;

    public BookServiceImpl(BookRepository bookRepository,
                           NotificationEventProducer notificationEventProducer) {
        this.bookRepository = bookRepository;
        this.notificationEventProducer = notificationEventProducer;
    }

    @Override
    public BookResponse addBook(BookRequest request) {
        validateBookRequest(request);

        if (bookRepository.findByIsbn(request.getIsbn()).isPresent()) {
            throw new BadRequestException("Book with this ISBN already exists");
        }

        Book book = mapToEntity(request);
        Book savedBook = bookRepository.save(book);

        checkAndSendLowStockAlert(savedBook);

        return mapToResponse(savedBook);
    }

    @Override
    public List<BookResponse> getAllBooks() {
        return bookRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BookResponse getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
        return mapToResponse(book);
    }

    @Override
    public List<BookResponse> searchBooks(String keyword) {
        return bookRepository
                .findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrGenreContainingIgnoreCase(
                        keyword, keyword, keyword)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookResponse> getByGenre(String genre) {
        return bookRepository.findByGenreIgnoreCase(genre)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BookResponse updateBook(Long id, BookRequest request) {
        validateBookRequest(request);

        Book existing = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        existing.setTitle(request.getTitle());
        existing.setAuthor(request.getAuthor());
        existing.setIsbn(request.getIsbn());
        existing.setGenre(request.getGenre());
        existing.setPublisher(request.getPublisher());
        existing.setPrice(request.getPrice());
        existing.setStock(request.getStock());
        existing.setRating(request.getRating());
        existing.setDescription(request.getDescription());
        existing.setCoverImageUrl(request.getCoverImageUrl());
        existing.setPublishedDate(request.getPublishedDate());
        existing.setFeatured(request.getFeatured());

        Book updatedBook = bookRepository.save(existing);
        checkAndSendLowStockAlert(updatedBook);

        return mapToResponse(updatedBook);
    }

    @Override
    public void deleteBook(Long id) {
        Book existing = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
        bookRepository.delete(existing);
    }

    @Override
    public BookResponse updateStock(Long id, Integer stock) {
        if (stock == null || stock < 0) {
            throw new BadRequestException("Stock cannot be negative");
        }

        Book existing = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        existing.setStock(stock);
        Book updatedBook = bookRepository.save(existing);

        checkAndSendLowStockAlert(updatedBook);

        return mapToResponse(updatedBook);
    }

    @Override
    public List<BookResponse> getFeaturedBooks() {
        return bookRepository.findByFeaturedTrue()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void checkAndSendLowStockAlert(Book book) {
        if (book.getStock() != null && book.getStock() <= LOW_STOCK_THRESHOLD) {
            NotificationEvent event = new NotificationEvent();
            event.setUserId(1L);
            event.setType("LOW_STOCK_ALERT");
            event.setMessage("Low stock alert: Book " + book.getTitle()
                    + " is running low. Remaining stock: " + book.getStock());

            notificationEventProducer.publish(event);
        }
    }

    private void validateBookRequest(BookRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException("Title is required");
        }
        if (request.getAuthor() == null || request.getAuthor().isBlank()) {
            throw new BadRequestException("Author is required");
        }
        if (request.getIsbn() == null || request.getIsbn().isBlank()) {
            throw new BadRequestException("ISBN is required");
        }
        if (request.getPrice() == null || request.getPrice().doubleValue() <= 0) {
            throw new BadRequestException("Price must be greater than 0");
        }
        if (request.getStock() == null || request.getStock() < 0) {
            throw new BadRequestException("Stock cannot be negative");
        }
    }

    private Book mapToEntity(BookRequest request) {
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setGenre(request.getGenre());
        book.setPublisher(request.getPublisher());
        book.setPrice(request.getPrice());
        book.setStock(request.getStock());
        book.setRating(request.getRating());
        book.setDescription(request.getDescription());
        book.setCoverImageUrl(request.getCoverImageUrl());
        book.setPublishedDate(request.getPublishedDate());
        book.setFeatured(request.getFeatured() != null ? request.getFeatured() : false);
        return book;
    }

    private BookResponse mapToResponse(Book book) {
        BookResponse response = new BookResponse();
        response.setBookId(book.getBookId());
        response.setTitle(book.getTitle());
        response.setAuthor(book.getAuthor());
        response.setIsbn(book.getIsbn());
        response.setGenre(book.getGenre());
        response.setPublisher(book.getPublisher());
        response.setPrice(book.getPrice());
        response.setStock(book.getStock());
        response.setRating(book.getRating());
        response.setDescription(book.getDescription());
        response.setCoverImageUrl(book.getCoverImageUrl());
        response.setPublishedDate(book.getPublishedDate());
        response.setFeatured(book.getFeatured());
        return response;
    }
}
