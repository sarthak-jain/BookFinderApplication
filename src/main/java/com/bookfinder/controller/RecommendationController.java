package com.bookfinder.controller;

import com.bookfinder.dto.RecommendationDTO;
import com.bookfinder.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/similar/{bookId}")
    public List<RecommendationDTO> getSimilar(
            @PathVariable String bookId,
            @RequestParam(defaultValue = "hybrid") String strategy,
            @RequestParam(defaultValue = "10") int limit) {
        return recommendationService.getSimilar(bookId, strategy, limit);
    }

    @GetMapping("/readers-also-liked/{bookId}")
    public List<RecommendationDTO> readersAlsoLiked(
            @PathVariable String bookId,
            @RequestParam(defaultValue = "10") int limit) {
        return recommendationService.readersAlsoLiked(bookId, limit);
    }

    @GetMapping("/shelf/{shelfName}")
    public List<RecommendationDTO> topInShelf(
            @PathVariable String shelfName,
            @RequestParam(defaultValue = "20") int limit) {
        return recommendationService.topInShelf(shelfName, limit);
    }

    @GetMapping("/author/{authorId}")
    public List<RecommendationDTO> moreByAuthor(
            @PathVariable String authorId,
            @RequestParam(defaultValue = "10") int limit) {
        return recommendationService.moreByAuthor(authorId, limit);
    }
}
