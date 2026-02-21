package com.bookfinder.controller;

import com.bookfinder.dto.*;
import com.bookfinder.service.GenreService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/genres")
public class GenreController {

    private final GenreService genreService;

    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping
    public List<GenreDTO> getGenres() {
        return genreService.getAllGenres();
    }

    @GetMapping("/{genreKey}/books")
    public PaginatedResponse<BookSearchResultDTO> getGenreBooks(
            @PathVariable String genreKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "ratingsCount") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        return genreService.getGenreBooks(genreKey, page, size, sortBy, direction);
    }

    @GetMapping("/{genreKey}/top-shelves")
    public List<ShelfDTO> getGenreTopShelves(
            @PathVariable String genreKey,
            @RequestParam(defaultValue = "20") int limit) {
        return genreService.getGenreTopShelves(genreKey, limit);
    }

    @GetMapping("/all/top-shelves")
    public List<ShelfDTO> getAllTopShelves(@RequestParam(defaultValue = "50") int limit) {
        return genreService.getAllTopShelves(limit);
    }
}
