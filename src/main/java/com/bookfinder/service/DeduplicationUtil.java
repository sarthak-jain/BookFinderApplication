package com.bookfinder.service;

import com.bookfinder.dto.BookSearchResultDTO;
import com.bookfinder.dto.RecommendationDTO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Removes exact duplicates (same bookId) and near-duplicates (same titleClean,
 * likely different editions). For near-duplicates, the copy with the highest
 * ratingsCount is kept so the most popular edition surfaces.
 */
public final class DeduplicationUtil {

    private DeduplicationUtil() {}

    public static List<BookSearchResultDTO> deduplicateBooks(List<BookSearchResultDTO> books) {
        if (books == null || books.size() <= 1) return books;

        LinkedHashMap<String, BookSearchResultDTO> seen = new LinkedHashMap<>();
        for (BookSearchResultDTO book : books) {
            String key = deduplicationKey(book.getTitleClean(), book.getTitle());
            if (key.isEmpty()) {
                // No usable title — fall back to bookId so we still keep it
                key = book.getBookId();
            }
            BookSearchResultDTO existing = seen.get(key);
            if (existing == null || ratings(book) > ratings(existing)) {
                seen.put(key, book);
            }
        }
        return new ArrayList<>(seen.values());
    }

    public static List<RecommendationDTO> deduplicateRecommendations(List<RecommendationDTO> recs) {
        if (recs == null || recs.size() <= 1) return recs;

        LinkedHashMap<String, RecommendationDTO> seen = new LinkedHashMap<>();
        for (RecommendationDTO rec : recs) {
            String key = deduplicationKey(rec.getTitleClean(), rec.getTitle());
            if (key.isEmpty()) {
                key = rec.getBookId();
            }
            RecommendationDTO existing = seen.get(key);
            if (existing == null || recRatings(rec) > recRatings(existing)) {
                seen.put(key, rec);
            }
        }
        return new ArrayList<>(seen.values());
    }

    private static String deduplicationKey(String titleClean, String title) {
        String t = titleClean != null && !titleClean.isBlank() ? titleClean : title;
        if (t == null || t.isBlank()) return "";
        return normalizeTitle(t);
    }

    private static String normalizeTitle(String t) {
        t = t.toLowerCase().strip();
        // Strip subtitles after colon, dash, or opening paren
        // e.g. "Let's Pretend This Never Happened: A Mostly True Memoir" -> "let's pretend this never happened"
        int colon = t.indexOf(':');
        if (colon > 0) t = t.substring(0, colon);
        int dash = t.indexOf(" - ");
        if (dash > 0) t = t.substring(0, dash);
        int paren = t.indexOf('(');
        if (paren > 0) t = t.substring(0, paren);
        return t.strip();
    }

    private static int ratings(BookSearchResultDTO b) {
        return b.getRatingsCount() != null ? b.getRatingsCount() : 0;
    }

    private static int recRatings(RecommendationDTO r) {
        return r.getRatingsCount() != null ? r.getRatingsCount() : 0;
    }
}
