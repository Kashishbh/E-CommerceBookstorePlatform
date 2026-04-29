package com.booknest.catalog.service;

import com.booknest.catalog.dto.BookRequest;
import com.booknest.catalog.dto.BookResponse;

import java.util.List;

public interface BookService {
    BookResponse addBook(BookRequest request);
    List<BookResponse> getAllBooks();
    BookResponse getBookById(Long id);
    List<BookResponse> searchBooks(String keyword);
    List<BookResponse> getByGenre(String genre);
    BookResponse updateBook(Long id, BookRequest request);
    void deleteBook(Long id);
    BookResponse updateStock(Long id, Integer stock);
    List<BookResponse> getFeaturedBooks();
    
}
