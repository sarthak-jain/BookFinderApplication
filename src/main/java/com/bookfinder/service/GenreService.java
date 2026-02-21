package com.bookfinder.service;

import com.bookfinder.dto.*;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.types.Node;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GenreService {

    private final Driver driver;

    public GenreService(Driver driver) {
        this.driver = driver;
    }

    public List<GenreDTO> getAllGenres() {
        try (Session session = session()) {
            var result = session.run("""
                MATCH (g:Genre)<-[:BELONGS_TO]-(b:Book)
                RETURN g.key AS key, g.name AS name, count(b) AS bookCount
                ORDER BY bookCount DESC
                """);

            List<GenreDTO> genres = new ArrayList<>();
            while (result.hasNext()) {
                Record rec = result.next();
                genres.add(new GenreDTO(
                        rec.get("key").asString(),
                        rec.get("name").asString(),
                        rec.get("bookCount").asLong()
                ));
            }
            return genres;
        }
    }

    public PaginatedResponse<BookSearchResultDTO> getGenreBooks(String genreKey, int page, int size,
                                                                  String sortBy, String direction) {
        String sortField = switch (sortBy) {
            case "title" -> "b.title";
            case "pubYear" -> "b.pubYear";
            case "averageRating" -> "b.averageRating";
            default -> "b.ratingsCount";
        };
        String dir = "ASC".equalsIgnoreCase(direction) ? "ASC" : "DESC";

        try (Session session = session()) {
            long total = session.run("""
                MATCH (b:Book {genre: $genre})
                RETURN count(b) AS cnt
                """, Map.of("genre", genreKey)).single().get("cnt").asLong();

            String query = String.format("""
                MATCH (b:Book {genre: $genre})
                RETURN b
                ORDER BY %s %s
                SKIP $skip LIMIT $limit
                """, sortField, dir);

            var result = session.run(query,
                    Map.of("genre", genreKey, "skip", (long) page * size, "limit", size));

            List<BookSearchResultDTO> books = new ArrayList<>();
            while (result.hasNext()) {
                books.add(toSearchResult(result.next().get("b").asNode()));
            }
            return new PaginatedResponse<>(books, page, size, total);
        }
    }

    public List<ShelfDTO> getGenreTopShelves(String genreKey, int limit) {
        try (Session session = session()) {
            var result = session.run("""
                MATCH (b:Book {genre: $genre})-[r:SHELVED_AS]->(s:Shelf)
                WITH s.name AS name, count(b) AS bookCount, sum(r.count) AS totalCount
                RETURN name, bookCount, totalCount
                ORDER BY bookCount DESC
                LIMIT $limit
                """, Map.of("genre", genreKey, "limit", limit));

            List<ShelfDTO> shelves = new ArrayList<>();
            while (result.hasNext()) {
                Record rec = result.next();
                shelves.add(new ShelfDTO(
                        rec.get("name").asString(),
                        (int) rec.get("bookCount").asLong()
                ));
            }
            return shelves;
        }
    }

    public List<ShelfDTO> getAllTopShelves(int limit) {
        try (Session session = session()) {
            var result = session.run("""
                MATCH (b:Book)-[r:SHELVED_AS]->(s:Shelf)
                WITH s.name AS name, count(b) AS bookCount
                RETURN name, bookCount
                ORDER BY bookCount DESC
                LIMIT $limit
                """, Map.of("limit", limit));

            List<ShelfDTO> shelves = new ArrayList<>();
            while (result.hasNext()) {
                Record rec = result.next();
                shelves.add(new ShelfDTO(
                        rec.get("name").asString(),
                        (int) rec.get("bookCount").asLong()
                ));
            }
            return shelves;
        }
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
