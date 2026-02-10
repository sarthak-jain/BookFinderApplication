package com.bookfinder.loader;

import com.fasterxml.jackson.databind.JsonNode;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class ReviewDataLoader {

    private static final Logger log = LoggerFactory.getLogger(ReviewDataLoader.class);

    private final Driver driver;
    private final String database;
    private final int batchSize;

    public ReviewDataLoader(Driver driver, String database, int batchSize) {
        this.driver = driver;
        this.database = database;
        this.batchSize = batchSize;
    }

    public void loadReviews(String filePath, Set<String> selectedBookIds, int maxReviews) throws IOException {
        log.info("Loading reviews (max {})...", maxReviews);

        List<Map<String, Object>> batch = new ArrayList<>(batchSize);
        int loaded = 0;
        int scanned = 0;

        try (JsonLineReader reader = new JsonLineReader(filePath)) {
            Iterator<JsonNode> it = reader.stream().iterator();
            while (it.hasNext() && loaded < maxReviews) {
                JsonNode node = it.next();
                scanned++;

                String bookId = node.path("book_id").asText("");
                if (!selectedBookIds.contains(bookId)) continue;

                String userId = node.path("user_id").asText("");
                String reviewText = node.path("review_text").asText("");
                if (userId.isEmpty() || reviewText.isEmpty()) continue;

                Map<String, Object> reviewMap = new HashMap<>();
                reviewMap.put("userId", userId);
                reviewMap.put("bookId", bookId);
                reviewMap.put("reviewId", node.path("review_id").asText(""));
                reviewMap.put("rating", node.path("rating").asInt(0));
                reviewMap.put("reviewText", truncate(reviewText, 500));
                reviewMap.put("nVotes", node.path("n_votes").asInt(0));
                reviewMap.put("nComments", node.path("n_comments").asInt(0));
                reviewMap.put("dateAdded", node.path("date_added").asText(""));
                batch.add(reviewMap);
                loaded++;

                if (batch.size() >= batchSize) {
                    flushReviews(batch);
                    batch.clear();
                    log.info("  Loaded {} reviews (scanned {})...", loaded, scanned);
                }
            }
        }

        if (!batch.isEmpty()) {
            flushReviews(batch);
        }

        log.info("Loaded {} reviews from {} scanned lines", loaded, scanned);
    }

    private void flushReviews(List<Map<String, Object>> batch) {
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            session.run("""
                UNWIND $batch AS r
                MERGE (u:User {userId: r.userId})
                WITH u, r
                MATCH (b:Book {bookId: r.bookId})
                MERGE (u)-[rev:REVIEWED]->(b)
                SET rev.reviewId = r.reviewId,
                    rev.rating = r.rating,
                    rev.reviewText = r.reviewText,
                    rev.nVotes = r.nVotes,
                    rev.nComments = r.nComments,
                    rev.dateAdded = r.dateAdded
                """, Map.of("batch", batch)).consume();
        }
    }

    private static String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
