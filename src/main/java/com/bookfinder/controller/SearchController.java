package com.bookfinder.controller;

import com.bookfinder.dto.BookSearchResultDTO;
import com.bookfinder.dto.PaginatedResponse;
import com.bookfinder.service.SearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public PaginatedResponse<BookSearchResultDTO> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Integer minYear,
            @RequestParam(required = false) Integer maxYear,
            @RequestParam(required = false) List<String> shelves) {
        return searchService.search(q, page, size, minRating, minYear, maxYear, shelves);
    }

    @GetMapping("/autocomplete")
    public List<BookSearchResultDTO> autocomplete(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit) {
        return searchService.autocomplete(q, limit);
    }
}
