package com.bookfinder.controller;

import com.bookfinder.dto.*;
import com.bookfinder.service.AuthorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/authors")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping("/{authorId}")
    public ResponseEntity<AuthorDTO> getAuthor(@PathVariable String authorId) {
        AuthorDTO author = authorService.getAuthor(authorId);
        return author != null ? ResponseEntity.ok(author) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{authorId}/books")
    public PaginatedResponse<BookSearchResultDTO> getAuthorBooks(
            @PathVariable String authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return authorService.getAuthorBooks(authorId, page, size);
    }
}
