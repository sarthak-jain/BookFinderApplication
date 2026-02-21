package com.bookfinder.service;

import com.bookfinder.dto.BookSearchResultDTO;
import com.bookfinder.dto.PaginatedResponse;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.types.Node;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SearchService {

    private final Driver driver;

    public SearchService(Driver driver) {
        this.driver = driver;
    }

    public PaginatedResponse<BookSearchResultDTO> search(String query, int page, int size,
                                                          Double minRating, Integer minYear,
                                                          Integer maxYear, List<String> shelves,
                                                          String genre) {
        String luceneQuery = sanitizeLuceneQuery(query);
        if (luceneQuery.isBlank()) {
            return new PaginatedResponse<>(List.of(), page, size, 0);
        }

        StringBuilder cypher = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        cypher.append("CALL db.index.fulltext.queryNodes('bookSearch', $query) YIELD node AS b, score\n");
        params.put("query", luceneQuery);

        List<String> filters = new ArrayList<>();
        if (minRating != null) {
            filters.add("b.averageRating >= $minRating");
            params.put("minRating", minRating);
        }
        if (minYear != null) {
            filters.add("b.pubYear >= $minYear");
            params.put("minYear", minYear);
        }
        if (maxYear != null) {
            filters.add("b.pubYear <= $maxYear");
            params.put("maxYear", maxYear);
        }
        if (genre != null && !genre.isBlank()) {
            filters.add("b.genre = $genre");
            params.put("genre", genre);
        }

        if (shelves != null && !shelves.isEmpty()) {
            cypher.append("MATCH (b)-[:SHELVED_AS]->(s:Shelf)\n");
            cypher.append("WHERE s.name IN $shelves\n");
            params.put("shelves", shelves);
            if (!filters.isEmpty()) {
                cypher.append("AND ").append(String.join(" AND ", filters)).append("\n");
            }
        } else if (!filters.isEmpty()) {
            cypher.append("WHERE ").append(String.join(" AND ", filters)).append("\n");
        }

        // Count query
        String countCypher = cypher + "RETURN count(DISTINCT b) AS cnt";
        // Data query
        String dataCypher = cypher + """
                RETURN DISTINCT b, score
                ORDER BY score DESC
                SKIP $skip LIMIT $limit
                """;

        params.put("skip", (long) page * size);
        params.put("limit", size);

        try (Session session = session()) {
            long total = session.run(countCypher, params).single().get("cnt").asLong();

            var result = session.run(dataCypher, params);
            List<BookSearchResultDTO> books = new ArrayList<>();
            while (result.hasNext()) {
                Record rec = result.next();
                Node node = rec.get("b").asNode();
                BookSearchResultDTO dto = toSearchResult(node);
                dto.setScore(rec.get("score").asDouble(0));
                books.add(dto);
            }
            return new PaginatedResponse<>(books, page, size, total);
        }
    }

    public List<BookSearchResultDTO> autocomplete(String query, int limit) {
        String luceneQuery = sanitizeLuceneQuery(query);
        if (luceneQuery.isBlank()) return List.of();

        // Add wildcard for partial matching
        String wildcardQuery = luceneQuery + "*";

        try (Session session = session()) {
            var result = session.run("""
                CALL db.index.fulltext.queryNodes('bookSearch', $query) YIELD node AS b, score
                RETURN b, score
                ORDER BY score DESC
                LIMIT $limit
                """, Map.of("query", wildcardQuery, "limit", limit));

            List<BookSearchResultDTO> books = new ArrayList<>();
            while (result.hasNext()) {
                Record rec = result.next();
                books.add(toSearchResult(rec.get("b").asNode()));
            }
            return books;
        }
    }

    private String sanitizeLuceneQuery(String query) {
        if (query == null) return "";
        // Escape Lucene special characters
        return query.replaceAll("[+\\-!(){}\\[\\]^\"~*?:\\\\/]", " ").trim();
    }

    private Session session() {
        return driver.session(SessionConfig.forDatabase("neo4j"));
    }

    private BookSearchResultDTO toSearchResult(Node node) {
        BookSearchResultDTO dto = new BookSearchResultDTO();
        dto.setBookId(node.get("bookId").asString(""));
        dto.setTitle(node.get("title").asString(""));
        dto.setTitleClean(node.get("titleClean").asString(""));
        dto.setAverageRating(node.get("averageRating").asDouble(0));
        dto.setRatingsCount(node.get("ratingsCount").asInt(0));
        dto.setImageUrl(node.get("imageUrl").asString(""));
        dto.setPublisher(node.get("publisher").asString(""));
        dto.setPubYear(node.get("pubYear").asInt(0));
        dto.setGenre(node.get("genre").asString(""));
        return dto;
    }
}
