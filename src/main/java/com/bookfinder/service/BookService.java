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
public class BookService {

    private final Driver driver;

    public BookService(Driver driver) {
        this.driver = driver;
    }

    public PaginatedResponse<BookSearchResultDTO> getBooks(int page, int size, String sortBy,
                                                            String direction, String genre) {
        String sortField = switch (sortBy) {
            case "title" -> "b.title";
            case "pubYear" -> "b.pubYear";
            case "averageRating" -> "b.averageRating";
            default -> "b.ratingsCount";
        };
        String dir = "ASC".equalsIgnoreCase(direction) ? "ASC" : "DESC";

        Map<String, Object> params = new HashMap<>();
        params.put("skip", (long) page * size);
        params.put("limit", size);

        String genreFilter = "";
        if (genre != null && !genre.isBlank()) {
            genreFilter = " {genre: $genre}";
            params.put("genre", genre);
        }

        try (Session session = session()) {
            long total = session.run(
                    String.format("MATCH (b:Book%s) RETURN count(b) AS cnt", genreFilter), params)
                    .single().get("cnt").asLong();

            String query = String.format("""
                MATCH (b:Book%s)
                RETURN b
                ORDER BY %s %s
                SKIP $skip LIMIT $limit
                """, genreFilter, sortField, dir);

            var result = session.run(query, params);
            List<BookSearchResultDTO> books = new ArrayList<>();
            while (result.hasNext()) {
                Node node = result.next().get("b").asNode();
                books.add(toSearchResult(node));
            }
            return new PaginatedResponse<>(books, page, size, total);
        }
    }

    public BookDTO getBookById(String bookId) {
        try (Session session = session()) {
            var result = session.run("""
                MATCH (b:Book {bookId: $bookId})
                OPTIONAL MATCH (a:Author)-[w:WROTE]->(b)
                OPTIONAL MATCH (b)-[sa:SHELVED_AS]->(s:Shelf)
                OPTIONAL MATCH (b)-[:IN_SERIES]->(ser:Series)
                RETURN b,
                       collect(DISTINCT {authorId: a.authorId, name: a.name, role: w.role}) AS authors,
                       collect(DISTINCT {name: s.name, count: sa.count}) AS shelves,
                       collect(DISTINCT ser.seriesId) AS seriesIds
                """, Map.of("bookId", bookId));

            if (!result.hasNext()) return null;

            Record record = result.next();
            Node bookNode = record.get("b").asNode();
            BookDTO dto = toBookDTO(bookNode);

            List<AuthorDTO> authors = new ArrayList<>();
            for (var a : record.get("authors").asList()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> aMap = (Map<String, Object>) a;
                if (aMap.get("authorId") != null) {
                    authors.add(new AuthorDTO(
                            Objects.toString(aMap.get("authorId"), ""),
                            Objects.toString(aMap.get("name"), ""),
                            Objects.toString(aMap.get("role"), "")
                    ));
                }
            }
            dto.setAuthors(authors);

            List<ShelfDTO> shelves = new ArrayList<>();
            for (var s : record.get("shelves").asList()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sMap = (Map<String, Object>) s;
                if (sMap.get("name") != null) {
                    shelves.add(new ShelfDTO(
                            Objects.toString(sMap.get("name"), ""),
                            sMap.get("count") instanceof Number n ? n.intValue() : 0
                    ));
                }
            }
            shelves.sort((a, b) -> Integer.compare(
                    b.getCount() != null ? b.getCount() : 0,
                    a.getCount() != null ? a.getCount() : 0
            ));
            dto.setShelves(shelves);

            List<String> seriesIds = new ArrayList<>();
            for (var s : record.get("seriesIds").asList()) {
                if (s != null) seriesIds.add(s.toString());
            }
            dto.setSeriesIds(seriesIds);

            return dto;
        }
    }

    public PaginatedResponse<ReviewDTO> getBookReviews(String bookId, int page, int size) {
        try (Session session = session()) {
            long total = session.run("""
                MATCH (u:User)-[r:REVIEWED]->(b:Book {bookId: $bookId})
                RETURN count(r) AS cnt
                """, Map.of("bookId", bookId)).single().get("cnt").asLong();

            var result = session.run("""
                MATCH (u:User)-[r:REVIEWED]->(b:Book {bookId: $bookId})
                RETURN u.userId AS userId, r.reviewId AS reviewId, r.rating AS rating,
                       r.reviewText AS reviewText, r.nVotes AS nVotes,
                       r.nComments AS nComments, r.dateAdded AS dateAdded
                ORDER BY r.nVotes DESC
                SKIP $skip LIMIT $limit
                """, Map.of("bookId", bookId, "skip", (long) page * size, "limit", size));

            List<ReviewDTO> reviews = new ArrayList<>();
            while (result.hasNext()) {
                Record rec = result.next();
                ReviewDTO dto = new ReviewDTO();
                dto.setReviewId(rec.get("reviewId").asString(""));
                dto.setUserId(rec.get("userId").asString(""));
                dto.setBookId(bookId);
                dto.setRating(rec.get("rating").asInt(0));
                dto.setReviewText(rec.get("reviewText").asString(""));
                dto.setNVotes(rec.get("nVotes").asInt(0));
                dto.setNComments(rec.get("nComments").asInt(0));
                dto.setDateAdded(rec.get("dateAdded").asString(""));
                reviews.add(dto);
            }
            return new PaginatedResponse<>(reviews, page, size, total);
        }
    }

    public List<BookSearchResultDTO> getSimilarBooks(String bookId, int limit) {
        try (Session session = session()) {
            var result = session.run("""
                MATCH (b:Book {bookId: $bookId})-[:SIMILAR_TO]->(sim:Book)
                RETURN sim
                ORDER BY sim.ratingsCount DESC
                LIMIT $limit
                """, Map.of("bookId", bookId, "limit", limit));

            List<BookSearchResultDTO> books = new ArrayList<>();
            while (result.hasNext()) {
                books.add(toSearchResult(result.next().get("sim").asNode()));
            }
            return books;
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

    private BookDTO toBookDTO(Node node) {
        BookDTO dto = new BookDTO();
        dto.setBookId(node.get("bookId").asString(""));
        dto.setTitle(node.get("title").asString(""));
        dto.setTitleClean(node.get("titleClean").asString(""));
        dto.setDescription(node.get("description").asString(""));
        dto.setAverageRating(node.get("averageRating").asDouble(0));
        dto.setRatingsCount(node.get("ratingsCount").asInt(0));
        dto.setNumPages(node.get("numPages").asInt(0));
        dto.setPublisher(node.get("publisher").asString(""));
        dto.setPubYear(node.get("pubYear").asInt(0));
        dto.setImageUrl(node.get("imageUrl").asString(""));
        dto.setUrl(node.get("url").asString(""));
        dto.setWorkId(node.get("workId").asString(""));
        dto.setGenre(node.get("genre").asString(""));
        return dto;
    }
}
