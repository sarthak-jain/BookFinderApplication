package com.bookfinder.loader;

import com.fasterxml.jackson.databind.JsonNode;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class InteractionDataLoader {

    private static final Logger log = LoggerFactory.getLogger(InteractionDataLoader.class);

    private final Driver driver;
    private final String database;
    private final int batchSize;

    public InteractionDataLoader(Driver driver, String database, int batchSize) {
        this.driver = driver;
        this.database = database;
        this.batchSize = batchSize;
    }

    public void loadInteractions(String filePath, Set<String> selectedBookIds, int maxInteractions) throws IOException {
        log.info("Loading interactions (max {})...", maxInteractions);

        List<Map<String, Object>> batch = new ArrayList<>(batchSize);
        int loaded = 0;
        int scanned = 0;

        try (JsonLineReader reader = new JsonLineReader(filePath)) {
            Iterator<JsonNode> it = reader.stream().iterator();
            while (it.hasNext() && loaded < maxInteractions) {
                JsonNode node = it.next();
                scanned++;

                String bookId = node.path("book_id").asText("");
                if (!selectedBookIds.contains(bookId)) continue;

                String userId = node.path("user_id").asText("");
                if (userId.isEmpty()) continue;

                Map<String, Object> interactionMap = new HashMap<>();
                interactionMap.put("userId", userId);
                interactionMap.put("bookId", bookId);
                interactionMap.put("isRead", node.path("is_read").asBoolean(false));
                interactionMap.put("rating", node.path("rating").asInt(0));
                interactionMap.put("dateAdded", node.path("date_added").asText(""));
                batch.add(interactionMap);
                loaded++;

                if (batch.size() >= batchSize) {
                    flushInteractions(batch);
                    batch.clear();
                    log.info("  Loaded {} interactions (scanned {})...", loaded, scanned);
                }

                if (scanned % 1000000 == 0) {
                    log.info("  Scanned {} interactions...", scanned);
                }
            }
        }

        if (!batch.isEmpty()) {
            flushInteractions(batch);
        }

        log.info("Loaded {} interactions from {} scanned lines", loaded, scanned);
    }

    private void flushInteractions(List<Map<String, Object>> batch) {
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            session.run("""
                UNWIND $batch AS i
                MERGE (u:User {userId: i.userId})
                WITH u, i
                MATCH (b:Book {bookId: i.bookId})
                MERGE (u)-[r:INTERACTED]->(b)
                SET r.isRead = i.isRead,
                    r.rating = i.rating,
                    r.dateAdded = i.dateAdded
                """, Map.of("batch", batch)).consume();
        }
    }
}
