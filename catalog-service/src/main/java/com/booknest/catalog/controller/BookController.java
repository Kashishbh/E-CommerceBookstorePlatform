package com.booknest.catalog.controller;

import com.booknest.catalog.dto.BookRequest;
import com.booknest.catalog.dto.BookResponse;
import com.booknest.catalog.dto.StockUpdateRequest;
import com.booknest.catalog.service.BookService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/books")
@CrossOrigin("*")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    public BookResponse addBook(@RequestBody BookRequest request) {
        return bookService.addBook(request);
    }

    @GetMapping
    public List<BookResponse> getAllBooks() {
        return bookService.getAllBooks();
    }

    @GetMapping("/{id}")
    public BookResponse getBookById(@PathVariable Long id) {
        return bookService.getBookById(id);
    }

    @GetMapping("/search")
    public List<BookResponse> searchBooks(@RequestParam String keyword) {
        return bookService.searchBooks(keyword);
    }

    @GetMapping("/genre/{genre}")
    public List<BookResponse> getByGenre(@PathVariable String genre) {
        return bookService.getByGenre(genre);
    }

    @GetMapping("/featured")
    public List<BookResponse> getFeaturedBooks() {
        return bookService.getFeaturedBooks();
    }

    @PutMapping("/{id}")
    public BookResponse updateBook(@PathVariable Long id, @RequestBody BookRequest request) {
        return bookService.updateBook(id, request);
    }

    @PutMapping("/{id}/stock")
    public BookResponse updateStock(@PathVariable Long id, @RequestBody StockUpdateRequest request) {
        return bookService.updateStock(id, request.getStock());
    }

    @DeleteMapping("/{id}")
    public String deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return "Book deleted successfully";
    }
}
