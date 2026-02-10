package com.bookfinder.controller;

import com.bookfinder.dto.*;
import com.bookfinder.service.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public PaginatedResponse<BookSearchResultDTO> getBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "ratingsCount") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        return bookService.getBooks(page, size, sortBy, direction);
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<BookDTO> getBook(@PathVariable String bookId) {
        BookDTO book = bookService.getBookById(bookId);
        return book != null ? ResponseEntity.ok(book) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{bookId}/reviews")
    public PaginatedResponse<ReviewDTO> getBookReviews(
            @PathVariable String bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return bookService.getBookReviews(bookId, page, size);
    }

    @GetMapping("/{bookId}/similar")
    public List<BookSearchResultDTO> getSimilarBooks(
            @PathVariable String bookId,
            @RequestParam(defaultValue = "10") int limit) {
        return bookService.getSimilarBooks(bookId, limit);
    }
}
