package com.bookfinder.loader;

import com.fasterxml.jackson.databind.JsonNode;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class AuthorMetadataLoader {

    private static final Logger log = LoggerFactory.getLogger(AuthorMetadataLoader.class);

    private final Driver driver;
    private final String database;
    private final int batchSize;

    public AuthorMetadataLoader(Driver driver, String database, int batchSize) {
        this.driver = driver;
        this.database = database;
        this.batchSize = batchSize;
    }

    public void loadAuthorNames(String authorsFilePath) throws IOException {
        log.info("Loading author metadata from {}...", authorsFilePath);

        List<Map<String, Object>> batch = new ArrayList<>(batchSize);
        int matched = 0;
        int scanned = 0;

        try (JsonLineReader reader = new JsonLineReader(authorsFilePath)) {
            Iterator<JsonNode> it = reader.stream().iterator();
            while (it.hasNext()) {
                JsonNode node = it.next();
                scanned++;

                String authorId = node.path("author_id").asText("");
                String name = node.path("name").asText("");
                if (authorId.isEmpty() || name.isEmpty()) continue;

                Map<String, Object> authorMap = new HashMap<>();
                authorMap.put("authorId", authorId);
                authorMap.put("name", name);
                batch.add(authorMap);

                if (batch.size() >= batchSize) {
                    matched += flushAuthorNames(batch);
                    batch.clear();
                }

                if (scanned % 100000 == 0) {
                    log.info("  Scanned {} authors, matched {} so far...", scanned, matched);
                }
            }
        }

        if (!batch.isEmpty()) {
            matched += flushAuthorNames(batch);
        }

        log.info("Author metadata loaded: scanned {}, updated {} authors with names", scanned, matched);
    }

    private int flushAuthorNames(List<Map<String, Object>> batch) {
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            var result = session.run("""
                UNWIND $batch AS a
                MATCH (author:Author {authorId: a.authorId})
                SET author.name = a.name
                RETURN count(author) AS updated
                """, Map.of("batch", batch));
            return result.single().get("updated").asInt();
        }
    }
}
