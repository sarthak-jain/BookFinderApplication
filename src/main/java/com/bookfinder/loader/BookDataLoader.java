package com.bookfinder.loader;

import com.fasterxml.jackson.databind.JsonNode;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class BookDataLoader {

    private static final Logger log = LoggerFactory.getLogger(BookDataLoader.class);
    private static final Set<String> ORGANIZATIONAL_SHELVES = Set.of(
            "to-read", "currently-reading", "owned", "books-i-own", "owned-books",
            "i-own", "my-books", "my-library", "have", "library", "kindle",
            "ebooks", "ebook", "e-book", "to-buy", "wish-list", "wishlist",
            "default", "favorites", "favourites", "re-read", "re-reads"
    );

    private final Driver driver;
    private final String database;
    private final int batchSize;

    public BookDataLoader(Driver driver, String database, int batchSize) {
        this.driver = driver;
        this.database = database;
        this.batchSize = batchSize;
    }

    public void loadBooks(String booksFilePath, Set<String> selectedBookIds) throws IOException {
        log.info("Loading {} books into Neo4j...", selectedBookIds.size());

        List<Map<String, Object>> batch = new ArrayList<>(batchSize);
        List<Map<String, Object>> authorBatch = new ArrayList<>();
        List<Map<String, Object>> seriesBatch = new ArrayList<>();
        List<Map<String, Object>> shelfBatch = new ArrayList<>();
        List<Map<String, Object>> similarBatch = new ArrayList<>();

        int loaded = 0;

        try (JsonLineReader reader = new JsonLineReader(booksFilePath)) {
            Iterator<JsonNode> it = reader.stream().iterator();
            while (it.hasNext()) {
                JsonNode node = it.next();
                String bookId = node.path("book_id").asText("");
                if (!selectedBookIds.contains(bookId)) continue;

                Map<String, Object> bookMap = new HashMap<>();
                bookMap.put("bookId", bookId);
                bookMap.put("title", node.path("title").asText(""));
                bookMap.put("titleClean", node.path("title_without_series").asText(""));
                bookMap.put("description", truncate(node.path("description").asText(""), 2000));
                bookMap.put("averageRating", parseDoubleSafe(node.path("average_rating").asText("0")));
                bookMap.put("ratingsCount", parseIntSafe(node.path("ratings_count").asText("0")));
                bookMap.put("numPages", parseIntSafe(node.path("num_pages").asText("0")));
                bookMap.put("publisher", node.path("publisher").asText(""));
                bookMap.put("pubYear", parseIntSafe(node.path("publication_year").asText("0")));
                bookMap.put("imageUrl", node.path("image_url").asText(""));
                bookMap.put("url", node.path("url").asText(""));
                bookMap.put("workId", node.path("work_id").asText(""));
                bookMap.put("isbn", node.path("isbn").asText(""));
                bookMap.put("isbn13", node.path("isbn13").asText(""));
                bookMap.put("asin", node.path("asin").asText(""));
                batch.add(bookMap);

                // Collect authors
                JsonNode authors = node.path("authors");
                if (authors.isArray()) {
                    for (JsonNode author : authors) {
                        Map<String, Object> authorMap = new HashMap<>();
                        authorMap.put("bookId", bookId);
                        authorMap.put("authorId", author.path("author_id").asText(""));
                        authorMap.put("role", author.path("role").asText(""));
                        authorBatch.add(authorMap);
                    }
                }

                // Collect series
                JsonNode seriesArr = node.path("series");
                if (seriesArr.isArray()) {
                    for (JsonNode s : seriesArr) {
                        String seriesId = s.asText("");
                        if (!seriesId.isEmpty()) {
                            Map<String, Object> seriesMap = new HashMap<>();
                            seriesMap.put("bookId", bookId);
                            seriesMap.put("seriesId", seriesId);
                            seriesBatch.add(seriesMap);
                        }
                    }
                }

                // Collect shelves (top 20, excluding organizational)
                JsonNode shelves = node.path("popular_shelves");
                if (shelves.isArray()) {
                    int shelfCount = 0;
                    for (JsonNode shelf : shelves) {
                        String shelfName = shelf.path("name").asText("");
                        if (ORGANIZATIONAL_SHELVES.contains(shelfName)) continue;
                        int count = parseIntSafe(shelf.path("count").asText("0"));
                        if (count < 2) continue;

                        Map<String, Object> shelfMap = new HashMap<>();
                        shelfMap.put("bookId", bookId);
                        shelfMap.put("name", shelfName);
                        shelfMap.put("count", count);
                        shelfBatch.add(shelfMap);
                        shelfCount++;
                        if (shelfCount >= 20) break;
                    }
                }

                // Collect similar books
                JsonNode similar = node.path("similar_books");
                if (similar.isArray()) {
                    for (JsonNode sim : similar) {
                        String simId = sim.asText("");
                        if (selectedBookIds.contains(simId)) {
                            Map<String, Object> simMap = new HashMap<>();
                            simMap.put("bookId", bookId);
                            simMap.put("similarBookId", simId);
                            similarBatch.add(simMap);
                        }
                    }
                }

                loaded++;
                if (batch.size() >= batchSize) {
                    flushBooks(batch);
                    batch.clear();
                    log.info("  Loaded {} books...", loaded);
                }
            }
        }

        if (!batch.isEmpty()) {
            flushBooks(batch);
        }
        log.info("Loaded {} book nodes", loaded);

        // Now flush authors, series, shelves, similar in batches
        flushAuthors(authorBatch);
        flushSeries(seriesBatch);
        flushShelves(shelfBatch);
        flushSimilar(similarBatch);
    }

    private void flushBooks(List<Map<String, Object>> batch) {
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            session.run("""
                UNWIND $batch AS b
                MERGE (book:Book {bookId: b.bookId})
                SET book.title = b.title,
                    book.titleClean = b.titleClean,
                    book.description = b.description,
                    book.averageRating = b.averageRating,
                    book.ratingsCount = b.ratingsCount,
                    book.numPages = b.numPages,
                    book.publisher = b.publisher,
                    book.pubYear = b.pubYear,
                    book.imageUrl = b.imageUrl,
                    book.url = b.url,
                    book.workId = b.workId,
                    book.isbn = b.isbn,
                    book.isbn13 = b.isbn13,
                    book.asin = b.asin
                """, Map.of("batch", batch)).consume();
        }
    }

    private void flushAuthors(List<Map<String, Object>> authorBatch) {
        log.info("Loading {} author relationships...", authorBatch.size());
        for (int i = 0; i < authorBatch.size(); i += batchSize) {
            List<Map<String, Object>> sub = authorBatch.subList(i, Math.min(i + batchSize, authorBatch.size()));
            try (Session session = driver.session(SessionConfig.forDatabase(database))) {
                session.run("""
                    UNWIND $batch AS a
                    MERGE (author:Author {authorId: a.authorId})
                    WITH author, a
                    MATCH (book:Book {bookId: a.bookId})
                    MERGE (author)-[r:WROTE]->(book)
                    SET r.role = a.role
                    """, Map.of("batch", sub)).consume();
            }
        }
        log.info("Loaded author relationships");
    }

    private void flushSeries(List<Map<String, Object>> seriesBatch) {
        log.info("Loading {} series relationships...", seriesBatch.size());
        for (int i = 0; i < seriesBatch.size(); i += batchSize) {
            List<Map<String, Object>> sub = seriesBatch.subList(i, Math.min(i + batchSize, seriesBatch.size()));
            try (Session session = driver.session(SessionConfig.forDatabase(database))) {
                session.run("""
                    UNWIND $batch AS s
                    MERGE (series:Series {seriesId: s.seriesId})
                    WITH series, s
                    MATCH (book:Book {bookId: s.bookId})
                    MERGE (book)-[:IN_SERIES]->(series)
                    """, Map.of("batch", sub)).consume();
            }
        }
        log.info("Loaded series relationships");
    }

    private void flushShelves(List<Map<String, Object>> shelfBatch) {
        log.info("Loading {} shelf relationships...", shelfBatch.size());
        for (int i = 0; i < shelfBatch.size(); i += batchSize) {
            List<Map<String, Object>> sub = shelfBatch.subList(i, Math.min(i + batchSize, shelfBatch.size()));
            try (Session session = driver.session(SessionConfig.forDatabase(database))) {
                session.run("""
                    UNWIND $batch AS s
                    MERGE (shelf:Shelf {name: s.name})
                    WITH shelf, s
                    MATCH (book:Book {bookId: s.bookId})
                    MERGE (book)-[r:SHELVED_AS]->(shelf)
                    SET r.count = s.count
                    """, Map.of("batch", sub)).consume();
            }
        }
        log.info("Loaded shelf relationships");
    }

    private void flushSimilar(List<Map<String, Object>> similarBatch) {
        log.info("Loading {} similar_to relationships...", similarBatch.size());
        for (int i = 0; i < similarBatch.size(); i += batchSize) {
            List<Map<String, Object>> sub = similarBatch.subList(i, Math.min(i + batchSize, similarBatch.size()));
            try (Session session = driver.session(SessionConfig.forDatabase(database))) {
                session.run("""
                    UNWIND $batch AS s
                    MATCH (b1:Book {bookId: s.bookId})
                    MATCH (b2:Book {bookId: s.similarBookId})
                    MERGE (b1)-[:SIMILAR_TO]->(b2)
                    """, Map.of("batch", sub)).consume();
            }
        }
        log.info("Loaded similar_to relationships");
    }

    private static String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    private static int parseIntSafe(String value) {
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private static double parseDoubleSafe(String value) {
        try { return Double.parseDouble(value.trim()); }
        catch (NumberFormatException e) { return 0.0; }
    }
}
