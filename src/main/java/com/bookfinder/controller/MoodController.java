package com.bookfinder.controller;

import com.bookfinder.dto.BookSearchResultDTO;
import com.bookfinder.dto.CustomMoodRequest;
import com.bookfinder.dto.MoodDTO;
import com.bookfinder.service.MoodService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/moods")
public class MoodController {

    private final MoodService moodService;

    public MoodController(MoodService moodService) {
        this.moodService = moodService;
    }

    @GetMapping
    public List<MoodDTO> getMoods() {
        return moodService.getAllMoods();
    }

    @GetMapping("/{moodKey}")
    public ResponseEntity<MoodDTO> getMood(@PathVariable String moodKey) {
        MoodDTO mood = moodService.getMood(moodKey);
        return mood != null ? ResponseEntity.ok(mood) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{moodKey}/books")
    public List<BookSearchResultDTO> getMoodBooks(
            @PathVariable String moodKey,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "all") String genre) {
        return moodService.getMoodBooks(moodKey, limit, genre);
    }

    @PostMapping("/custom/books")
    public List<BookSearchResultDTO> getCustomMoodBooks(@RequestBody CustomMoodRequest request) {
        return moodService.getCustomMoodBooks(
                request.getShelves(), request.getLimit(), request.getGenre());
    }
}
