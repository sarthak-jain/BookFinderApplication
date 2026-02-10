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
public class AuthorService {

    private final Driver driver;

    public AuthorService(Driver driver) {
        this.driver = driver;
    }

    public AuthorDTO getAuthor(String authorId) {
        try (Session session = session()) {
            var result = session.run("""
                MATCH (a:Author {authorId: $authorId})
                RETURN a
                """, Map.of("authorId", authorId));

            if (!result.hasNext()) return null;

            Node node = result.next().get("a").asNode();
            return new AuthorDTO(node.get("authorId").asString(""), node.get("role").asString(""));
        }
    }

    public PaginatedResponse<BookSearchResultDTO> getAuthorBooks(String authorId, int page, int size) {
        try (Session session = session()) {
            long total = session.run("""
                MATCH (a:Author {authorId: $authorId})-[:WROTE]->(b:Book)
                RETURN count(b) AS cnt
                """, Map.of("authorId", authorId)).single().get("cnt").asLong();

            var result = session.run("""
                MATCH (a:Author {authorId: $authorId})-[:WROTE]->(b:Book)
                RETURN b
                ORDER BY b.ratingsCount DESC
                SKIP $skip LIMIT $limit
                """, Map.of("authorId", authorId, "skip", (long) page * size, "limit", size));

            List<BookSearchResultDTO> books = new ArrayList<>();
            while (result.hasNext()) {
                Node node = result.next().get("b").asNode();
                BookSearchResultDTO dto = new BookSearchResultDTO();
                dto.setBookId(node.get("bookId").asString(""));
                dto.setTitle(node.get("title").asString(""));
                dto.setTitleClean(node.get("titleClean").asString(""));
                dto.setAverageRating(node.get("averageRating").asDouble(0));
                dto.setRatingsCount(node.get("ratingsCount").asInt(0));
                dto.setImageUrl(node.get("imageUrl").asString(""));
                dto.setPublisher(node.get("publisher").asString(""));
                dto.setPubYear(node.get("pubYear").asInt(0));
                books.add(dto);
            }
            return new PaginatedResponse<>(books, page, size, total);
        }
    }

    private Session session() {
        return driver.session(SessionConfig.forDatabase("neo4j"));
    }
}
