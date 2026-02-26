package com.bookfinder.service;

import com.bookfinder.dto.RecommendationDTO;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.types.Node;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final Driver driver;

    public RecommendationService(Driver driver) {
        this.driver = driver;
    }

    public List<RecommendationDTO> getSimilar(String bookId, String strategy, int limit) {
        return switch (strategy) {
            case "graph" -> graphSimilarity(bookId, limit);
            case "shelf" -> shelfSimilarity(bookId, limit);
            case "collaborative" -> collaborativeFiltering(bookId, limit);
            case "hybrid" -> hybridRecommendation(bookId, limit);
            default -> graphSimilarity(bookId, limit);
        };
    }

    public List<RecommendationDTO> readersAlsoLiked(String bookId, int limit) {
        return collaborativeFiltering(bookId, limit);
    }

    public List<RecommendationDTO> topInShelf(String shelfName, int limit) {
        try (Session session = session()) {
            var result = session.run("""
                MATCH (b:Book)-[:SHELVED_AS]->(s:Shelf {name: $shelfName})
                RETURN DISTINCT b
                ORDER BY b.ratingsCount DESC
                LIMIT $limit
                """, Map.of("shelfName", shelfName, "limit", limit));

            List<RecommendationDTO> recs = new ArrayList<>();
            while (result.hasNext()) {
                Node node = result.next().get("b").asNode();
                RecommendationDTO dto = toRecommendation(node);
                dto.setStrategy("shelf");
                dto.setScore((double) node.get("ratingsCount").asInt(0));
                recs.add(dto);
            }
            return DeduplicationUtil.deduplicateRecommendations(recs);
        }
    }

    public List<RecommendationDTO> moreByAuthor(String authorId, int limit) {
        try (Session session = session()) {
            var result = session.run("""
                MATCH (a:Author {authorId: $authorId})-[:WROTE]->(b:Book)
                RETURN DISTINCT b
                ORDER BY b.ratingsCount DESC
                LIMIT $limit
                """, Map.of("authorId", authorId, "limit", limit));

            List<RecommendationDTO> recs = new ArrayList<>();
            while (result.hasNext()) {
                Node node = result.next().get("b").asNode();
                RecommendationDTO dto = toRecommendation(node);
                dto.setStrategy("author");
                recs.add(dto);
            }
            return DeduplicationUtil.deduplicateRecommendations(recs);
        }
    }

    private List<RecommendationDTO> graphSimilarity(String bookId, int limit) {
        try (Session session = session()) {
            // 1-hop and 2-hop similar books
            var result = session.run("""
                MATCH (b:Book {bookId: $bookId})-[:SIMILAR_TO*1..2]->(rec:Book)
                WHERE rec.bookId <> $bookId
                WITH rec, count(*) AS paths
                RETURN rec, paths
                ORDER BY paths DESC, rec.ratingsCount DESC
                LIMIT $limit
                """, Map.of("bookId", bookId, "limit", limit));

            List<RecommendationDTO> recs = new ArrayList<>();
            while (result.hasNext()) {
                Record rec = result.next();
                Node node = rec.get("rec").asNode();
                RecommendationDTO dto = toRecommendation(node);
                dto.setStrategy("graph");
                dto.setScore(rec.get("paths").asDouble(1));
                recs.add(dto);
            }
            return DeduplicationUtil.deduplicateRecommendations(recs);
        }
    }

    private List<RecommendationDTO> shelfSimilarity(String bookId, int limit) {
        try (Session session = session()) {
            var result = session.run("""
                MATCH (b:Book {bookId: $bookId})-[:SHELVED_AS]->(s:Shelf)<-[:SHELVED_AS]-(rec:Book)
                WHERE rec.bookId <> $bookId
                WITH rec, count(DISTINCT s) AS sharedShelves
                WHERE sharedShelves >= 3
                RETURN rec, sharedShelves
                ORDER BY sharedShelves DESC, rec.ratingsCount DESC
                LIMIT $limit
                """, Map.of("bookId", bookId, "limit", limit));

            List<RecommendationDTO> recs = new ArrayList<>();
            while (result.hasNext()) {
                Record rec = result.next();
                Node node = rec.get("rec").asNode();
                RecommendationDTO dto = toRecommendation(node);
                dto.setStrategy("shelf");
                dto.setScore(rec.get("sharedShelves").asDouble(0));
                recs.add(dto);
            }
            return DeduplicationUtil.deduplicateRecommendations(recs);
        }
    }

    private List<RecommendationDTO> collaborativeFiltering(String bookId, int limit) {
        try (Session session = session()) {
            var result = session.run("""
                MATCH (b:Book {bookId: $bookId})<-[i1:INTERACTED]-(u:User)-[i2:INTERACTED]->(rec:Book)
                WHERE i1.rating >= 4 AND i2.rating >= 4 AND rec.bookId <> $bookId
                WITH rec, count(DISTINCT u) AS commonUsers
                RETURN rec, commonUsers
                ORDER BY commonUsers DESC, rec.averageRating DESC
                LIMIT $limit
                """, Map.of("bookId", bookId, "limit", limit));

            List<RecommendationDTO> recs = new ArrayList<>();
            while (result.hasNext()) {
                Record rec = result.next();
                Node node = rec.get("rec").asNode();
                RecommendationDTO dto = toRecommendation(node);
                dto.setStrategy("collaborative");
                dto.setScore(rec.get("commonUsers").asDouble(0));
                recs.add(dto);
            }
            return DeduplicationUtil.deduplicateRecommendations(recs);
        }
    }

    private List<RecommendationDTO> hybridRecommendation(String bookId, int limit) {
        // Get results from all strategies
        List<RecommendationDTO> graphRecs = graphSimilarity(bookId, limit * 2);
        List<RecommendationDTO> shelfRecs = shelfSimilarity(bookId, limit * 2);
        List<RecommendationDTO> collabRecs = collaborativeFiltering(bookId, limit * 2);

        // Normalize scores within each strategy
        normalizeScores(graphRecs);
        normalizeScores(shelfRecs);
        normalizeScores(collabRecs);

        // Merge with weights
        Map<String, Double> combinedScores = new HashMap<>();
        Map<String, RecommendationDTO> bookMap = new HashMap<>();

        for (var rec : graphRecs) {
            combinedScores.merge(rec.getBookId(), 0.4 * rec.getScore(), Double::sum);
            bookMap.put(rec.getBookId(), rec);
        }
        for (var rec : shelfRecs) {
            combinedScores.merge(rec.getBookId(), 0.3 * rec.getScore(), Double::sum);
            bookMap.putIfAbsent(rec.getBookId(), rec);
        }
        for (var rec : collabRecs) {
            combinedScores.merge(rec.getBookId(), 0.3 * rec.getScore(), Double::sum);
            bookMap.putIfAbsent(rec.getBookId(), rec);
        }

        List<RecommendationDTO> merged = combinedScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(e -> {
                    RecommendationDTO dto = bookMap.get(e.getKey());
                    dto.setScore(e.getValue());
                    dto.setStrategy("hybrid");
                    return dto;
                })
                .collect(Collectors.toList());
        return DeduplicationUtil.deduplicateRecommendations(merged);
    }

    private void normalizeScores(List<RecommendationDTO> recs) {
        if (recs.isEmpty()) return;
        double maxScore = recs.stream().mapToDouble(r -> r.getScore() != null ? r.getScore() : 0).max().orElse(1);
        if (maxScore == 0) maxScore = 1;
        for (var rec : recs) {
            rec.setScore((rec.getScore() != null ? rec.getScore() : 0) / maxScore);
        }
    }

    private Session session() {
        return driver.session(SessionConfig.forDatabase("neo4j"));
    }

    private RecommendationDTO toRecommendation(Node node) {
        RecommendationDTO dto = new RecommendationDTO();
        dto.setBookId(node.get("bookId").asString(""));
        dto.setTitle(node.get("title").asString(""));
        dto.setTitleClean(node.get("titleClean").asString(""));
        dto.setAverageRating(node.get("averageRating").asDouble(0));
        dto.setRatingsCount(node.get("ratingsCount").asInt(0));
        dto.setImageUrl(node.get("imageUrl").asString(""));
        dto.setPubYear(node.get("pubYear").asInt(0));
        dto.setGenre(node.get("genre").asString(""));
        return dto;
    }
}
