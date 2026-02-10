package com.bookfinder.service;

import com.bookfinder.dto.GraphVisualizationDTO;
import com.bookfinder.dto.GraphVisualizationDTO.EdgeDTO;
import com.bookfinder.dto.GraphVisualizationDTO.NodeDTO;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GraphService {

    private static final String COLOR_BOOK = "#4A90D9";
    private static final String COLOR_AUTHOR = "#E8913A";
    private static final String COLOR_SHELF = "#5CB85C";
    private static final String COLOR_SERIES = "#9B59B6";
    private static final String COLOR_USER = "#95A5A6";
    private static final String COLOR_EDGE = "#CCCCCC";

    private final Driver driver;

    public GraphService(Driver driver) {
        this.driver = driver;
    }

    public GraphVisualizationDTO bookNeighborhood(String bookId, int depth, boolean includeUsers) {
        Map<String, NodeDTO> nodes = new LinkedHashMap<>();
        List<EdgeDTO> edges = new ArrayList<>();

        try (Session session = session()) {
            // Book + authors + shelves + series + similar
            var result = session.run("""
                MATCH (b:Book {bookId: $bookId})
                OPTIONAL MATCH path1 = (a:Author)-[:WROTE]->(b)
                OPTIONAL MATCH path2 = (b)-[:SHELVED_AS]->(s:Shelf)
                OPTIONAL MATCH path3 = (b)-[:IN_SERIES]->(ser:Series)
                OPTIONAL MATCH path4 = (b)-[:SIMILAR_TO]->(sim:Book)
                RETURN b, collect(DISTINCT a) AS authors, collect(DISTINCT s) AS shelves,
                       collect(DISTINCT ser) AS series, collect(DISTINCT sim) AS similarBooks
                """, Map.of("bookId", bookId));

            if (!result.hasNext()) return new GraphVisualizationDTO(List.of(), List.of());

            Record rec = result.next();
            Node bookNode = rec.get("b").asNode();
            addBookNode(nodes, bookNode, 30);

            for (var a : rec.get("authors").asList(v -> v.asNode())) {
                String aid = "author_" + a.get("authorId").asString("");
                nodes.put(aid, new NodeDTO(aid, "Author " + a.get("authorId").asString(""), "Author", COLOR_AUTHOR, 20, Map.of("authorId", a.get("authorId").asString(""))));
                edges.add(new EdgeDTO(aid, "book_" + bookId, "WROTE", COLOR_EDGE));
            }

            for (var s : rec.get("shelves").asList(v -> v.asNode())) {
                String sid = "shelf_" + s.get("name").asString("");
                nodes.put(sid, new NodeDTO(sid, s.get("name").asString(""), "Shelf", COLOR_SHELF, 15, null));
                edges.add(new EdgeDTO("book_" + bookId, sid, "SHELVED_AS", COLOR_EDGE));
            }

            for (var ser : rec.get("series").asList(v -> v.asNode())) {
                String sid = "series_" + ser.get("seriesId").asString("");
                nodes.put(sid, new NodeDTO(sid, "Series " + ser.get("seriesId").asString(""), "Series", COLOR_SERIES, 18, null));
                edges.add(new EdgeDTO("book_" + bookId, sid, "IN_SERIES", COLOR_EDGE));
            }

            for (var sim : rec.get("similarBooks").asList(v -> v.asNode())) {
                addBookNode(nodes, sim, 20);
                edges.add(new EdgeDTO("book_" + bookId, "book_" + sim.get("bookId").asString(""), "SIMILAR_TO", COLOR_EDGE));

                // Depth 2: get similar books of similar
                if (depth >= 2) {
                    var depth2 = session.run("""
                        MATCH (b:Book {bookId: $simId})-[:SIMILAR_TO]->(sim2:Book)
                        WHERE sim2.bookId <> $bookId
                        RETURN sim2 LIMIT 3
                        """, Map.of("simId", sim.get("bookId").asString(""), "bookId", bookId));
                    while (depth2.hasNext()) {
                        Node sim2 = depth2.next().get("sim2").asNode();
                        addBookNode(nodes, sim2, 15);
                        edges.add(new EdgeDTO("book_" + sim.get("bookId").asString(""), "book_" + sim2.get("bookId").asString(""), "SIMILAR_TO", COLOR_EDGE));
                    }
                }
            }

            // Include users if requested
            if (includeUsers) {
                var userResult = session.run("""
                    MATCH (u:User)-[r:INTERACTED]->(b:Book {bookId: $bookId})
                    WHERE r.rating >= 4
                    RETURN u LIMIT 10
                    """, Map.of("bookId", bookId));
                while (userResult.hasNext()) {
                    Node user = userResult.next().get("u").asNode();
                    String uid = "user_" + user.get("userId").asString("");
                    nodes.put(uid, new NodeDTO(uid, "User", "User", COLOR_USER, 10, null));
                    edges.add(new EdgeDTO(uid, "book_" + bookId, "INTERACTED", COLOR_EDGE));
                }
            }
        }

        return new GraphVisualizationDTO(new ArrayList<>(nodes.values()), edges);
    }

    public GraphVisualizationDTO authorGraph(String authorId) {
        Map<String, NodeDTO> nodes = new LinkedHashMap<>();
        List<EdgeDTO> edges = new ArrayList<>();

        try (Session session = session()) {
            var result = session.run("""
                MATCH (a:Author {authorId: $authorId})-[:WROTE]->(b:Book)
                OPTIONAL MATCH (b)-[:IN_SERIES]->(ser:Series)
                RETURN a, b, collect(DISTINCT ser) AS series
                """, Map.of("authorId", authorId));

            String aid = "author_" + authorId;
            nodes.put(aid, new NodeDTO(aid, "Author " + authorId, "Author", COLOR_AUTHOR, 30, Map.of("authorId", authorId)));

            while (result.hasNext()) {
                Record rec = result.next();
                Node bookNode = rec.get("b").asNode();
                addBookNode(nodes, bookNode, 20);
                edges.add(new EdgeDTO(aid, "book_" + bookNode.get("bookId").asString(""), "WROTE", COLOR_EDGE));

                for (var ser : rec.get("series").asList(v -> v.asNode())) {
                    String sid = "series_" + ser.get("seriesId").asString("");
                    nodes.putIfAbsent(sid, new NodeDTO(sid, "Series " + ser.get("seriesId").asString(""), "Series", COLOR_SERIES, 18, null));
                    edges.add(new EdgeDTO("book_" + bookNode.get("bookId").asString(""), sid, "IN_SERIES", COLOR_EDGE));
                }
            }
        }

        return new GraphVisualizationDTO(new ArrayList<>(nodes.values()), edges);
    }

    public GraphVisualizationDTO shelfGraph(String shelfName, int limit) {
        Map<String, NodeDTO> nodes = new LinkedHashMap<>();
        List<EdgeDTO> edges = new ArrayList<>();

        try (Session session = session()) {
            String sid = "shelf_" + shelfName;
            nodes.put(sid, new NodeDTO(sid, shelfName, "Shelf", COLOR_SHELF, 30, null));

            var result = session.run("""
                MATCH (b:Book)-[:SHELVED_AS]->(s:Shelf {name: $shelfName})
                RETURN b
                ORDER BY b.ratingsCount DESC
                LIMIT $limit
                """, Map.of("shelfName", shelfName, "limit", limit));

            while (result.hasNext()) {
                Node bookNode = result.next().get("b").asNode();
                addBookNode(nodes, bookNode, 20);
                edges.add(new EdgeDTO("book_" + bookNode.get("bookId").asString(""), sid, "SHELVED_AS", COLOR_EDGE));
            }
        }

        return new GraphVisualizationDTO(new ArrayList<>(nodes.values()), edges);
    }

    public GraphVisualizationDTO recommendationGraph(String bookId) {
        Map<String, NodeDTO> nodes = new LinkedHashMap<>();
        List<EdgeDTO> edges = new ArrayList<>();

        try (Session session = session()) {
            // Source book
            var sourceResult = session.run("MATCH (b:Book {bookId: $bookId}) RETURN b", Map.of("bookId", bookId));
            if (!sourceResult.hasNext()) return new GraphVisualizationDTO(List.of(), List.of());
            addBookNode(nodes, sourceResult.next().get("b").asNode(), 35);

            // SIMILAR_TO recommendations
            var simResult = session.run("""
                MATCH (b:Book {bookId: $bookId})-[:SIMILAR_TO]->(rec:Book)
                RETURN rec LIMIT 8
                """, Map.of("bookId", bookId));
            while (simResult.hasNext()) {
                Node rec = simResult.next().get("rec").asNode();
                addBookNode(nodes, rec, 20);
                edges.add(new EdgeDTO("book_" + bookId, "book_" + rec.get("bookId").asString(""), "SIMILAR_TO", "#4A90D9"));
            }

            // Shelf-based recommendations
            var shelfResult = session.run("""
                MATCH (b:Book {bookId: $bookId})-[:SHELVED_AS]->(s:Shelf)<-[:SHELVED_AS]-(rec:Book)
                WHERE rec.bookId <> $bookId
                WITH rec, collect(DISTINCT s.name) AS shelves, count(DISTINCT s) AS cnt
                WHERE cnt >= 3
                RETURN rec, shelves
                ORDER BY cnt DESC
                LIMIT 5
                """, Map.of("bookId", bookId));
            while (shelfResult.hasNext()) {
                Record rec = shelfResult.next();
                Node recNode = rec.get("rec").asNode();
                addBookNode(nodes, recNode, 18);
                edges.add(new EdgeDTO("book_" + bookId, "book_" + recNode.get("bookId").asString(""), "SHELF_SIMILAR", "#5CB85C"));
            }

            // Collaborative recommendations
            var collabResult = session.run("""
                MATCH (b:Book {bookId: $bookId})<-[i1:INTERACTED]-(u:User)-[i2:INTERACTED]->(rec:Book)
                WHERE i1.rating >= 4 AND i2.rating >= 4 AND rec.bookId <> $bookId
                WITH rec, count(DISTINCT u) AS commonUsers
                RETURN rec, commonUsers
                ORDER BY commonUsers DESC
                LIMIT 5
                """, Map.of("bookId", bookId));
            while (collabResult.hasNext()) {
                Record rec = collabResult.next();
                Node recNode = rec.get("rec").asNode();
                addBookNode(nodes, recNode, 18);
                edges.add(new EdgeDTO("book_" + bookId, "book_" + recNode.get("bookId").asString(""), "READERS_ALSO_LIKED", "#E8913A"));
            }
        }

        return new GraphVisualizationDTO(new ArrayList<>(nodes.values()), edges);
    }

    private void addBookNode(Map<String, NodeDTO> nodes, Node bookNode, int size) {
        String id = "book_" + bookNode.get("bookId").asString("");
        if (!nodes.containsKey(id)) {
            String title = bookNode.get("title").asString("");
            String shortTitle = title.length() > 30 ? title.substring(0, 27) + "..." : title;
            nodes.put(id, new NodeDTO(id, shortTitle, "Book", COLOR_BOOK, size,
                    Map.of(
                            "bookId", bookNode.get("bookId").asString(""),
                            "title", title,
                            "averageRating", bookNode.get("averageRating").asDouble(0),
                            "imageUrl", bookNode.get("imageUrl").asString("")
                    )));
        }
    }

    private Session session() {
        return driver.session(SessionConfig.forDatabase("neo4j"));
    }
}
