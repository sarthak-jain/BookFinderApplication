package com.bookfinder.controller;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final Driver driver;

    public HealthController(Driver driver) {
        this.driver = driver;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        try (Session session = driver.session(SessionConfig.forDatabase("neo4j"))) {
            var result = session.run("MATCH (n) RETURN count(n) AS nodeCount");
            long nodeCount = result.single().get("nodeCount").asLong();
            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "neo4j", "connected",
                    "nodeCount", nodeCount
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "neo4j", "error: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        try (Session session = driver.session(SessionConfig.forDatabase("neo4j"))) {
            var nodeResult = session.run("""
                MATCH (n)
                RETURN labels(n)[0] AS label, count(n) AS count
                ORDER BY count DESC
                """);
            var nodeCounts = new java.util.LinkedHashMap<String, Long>();
            while (nodeResult.hasNext()) {
                var record = nodeResult.next();
                nodeCounts.put(record.get("label").asString(), record.get("count").asLong());
            }

            var relResult = session.run("""
                MATCH ()-[r]->()
                RETURN type(r) AS type, count(r) AS count
                ORDER BY count DESC
                """);
            var relCounts = new java.util.LinkedHashMap<String, Long>();
            while (relResult.hasNext()) {
                var record = relResult.next();
                relCounts.put(record.get("type").asString(), record.get("count").asLong());
            }

            return ResponseEntity.ok(Map.of(
                    "nodes", nodeCounts,
                    "relationships", relCounts
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
