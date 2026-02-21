package com.bookfinder.loader;

import com.bookfinder.config.DataLoadProperties;
import com.bookfinder.config.DataLoadProperties.GenreConfig;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Profile("load-data")
public class DataLoaderRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoaderRunner.class);

    private final Driver driver;
    private final DataLoadProperties props;

    public DataLoaderRunner(Driver driver, DataLoadProperties props) {
        this.driver = driver;
        this.props = props;
    }

    @Override
    public void run(String... args) throws Exception {
        long start = System.currentTimeMillis();
        String database = "neo4j";

        log.info("=== BookFinder Multi-Genre Data Loading Pipeline ===");
        log.info("Data directory: {}", props.getDir());
        log.info("Genres to load: {}", props.getGenres().size());

        // Step 0: Clear existing data
        clearDatabase(database);

        // Step 1: Create constraints and indexes
        createConstraints(database);

        // Step 2: Load each genre
        BookDataLoader bookLoader = new BookDataLoader(driver, database, props.getBatchSize());
        InteractionDataLoader interactionLoader = new InteractionDataLoader(driver, database, props.getBatchSize());
        ReviewDataLoader reviewLoader = new ReviewDataLoader(driver, database, props.getBatchSize());

        for (GenreConfig genre : props.getGenres()) {
            long genreStart = System.currentTimeMillis();
            log.info("--- Loading genre: {} ({}) ---", genre.getName(), genre.getKey());
            log.info("  Subset size: {}, Max interactions: {}, Max reviews: {}",
                    genre.getSubsetSize(), genre.getMaxInteractions(), genre.getMaxReviews());

            String dataDir = props.getDir();

            // Select top books by ratings_count for this genre
            Set<String> selectedBookIds = SubsetSelector.selectTopBookIds(
                    genre.getBooksPath(dataDir), genre.getSubsetSize()
            );

            // Load books (nodes + authors + series + shelves + similar + genre relationship)
            bookLoader.loadBooks(
                    genre.getBooksPath(dataDir), selectedBookIds,
                    genre.getKey(), genre.getName()
            );

            // Load interactions
            interactionLoader.loadInteractions(
                    genre.getInteractionsPath(dataDir), selectedBookIds,
                    genre.getMaxInteractions()
            );

            // Load reviews
            reviewLoader.loadReviews(
                    genre.getReviewsPath(dataDir), selectedBookIds,
                    genre.getMaxReviews()
            );

            long genreElapsed = (System.currentTimeMillis() - genreStart) / 1000;
            log.info("--- Genre '{}' loaded in {} seconds ---", genre.getName(), genreElapsed);
        }

        // Step 3: Load author metadata (names) across all genres
        log.info("Loading author metadata...");
        AuthorMetadataLoader authorLoader = new AuthorMetadataLoader(driver, database, props.getBatchSize());
        authorLoader.loadAuthorNames(props.getAuthorsPath());

        // Step 4: Create full-text indexes
        createFullTextIndex(database);

        long elapsed = (System.currentTimeMillis() - start) / 1000;
        log.info("=== Data loading complete in {} seconds ===", elapsed);

        // Print summary
        printSummary(database);
    }

    private void clearDatabase(String database) {
        log.info("Clearing existing data...");
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            // Drop full-text index first (can't drop while data is being deleted)
            try {
                session.run("DROP INDEX bookSearch IF EXISTS").consume();
                log.info("Dropped bookSearch index");
            } catch (Exception e) {
                log.info("No bookSearch index to drop");
            }

            // Delete in batches to avoid memory issues on AuraDB
            long totalDeleted = 0;
            long deleted = 1;
            while (deleted > 0) {
                var result = session.run(
                        "MATCH (n) WITH n LIMIT 10000 DETACH DELETE n RETURN count(n) AS cnt");
                deleted = result.single().get("cnt").asLong();
                totalDeleted += deleted;
                if (totalDeleted % 50000 == 0 && totalDeleted > 0) {
                    log.info("  Deleted {} nodes so far...", totalDeleted);
                }
            }
            log.info("Cleared {} nodes from database", totalDeleted);
        }
    }

    private void createConstraints(String database) {
        log.info("Creating constraints and indexes...");
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            session.run("CREATE CONSTRAINT book_id IF NOT EXISTS FOR (b:Book) REQUIRE b.bookId IS UNIQUE").consume();
            session.run("CREATE CONSTRAINT author_id IF NOT EXISTS FOR (a:Author) REQUIRE a.authorId IS UNIQUE").consume();
            session.run("CREATE CONSTRAINT user_id IF NOT EXISTS FOR (u:User) REQUIRE u.userId IS UNIQUE").consume();
            session.run("CREATE CONSTRAINT shelf_name IF NOT EXISTS FOR (s:Shelf) REQUIRE s.name IS UNIQUE").consume();
            session.run("CREATE CONSTRAINT series_id IF NOT EXISTS FOR (s:Series) REQUIRE s.seriesId IS UNIQUE").consume();
            session.run("CREATE CONSTRAINT genre_key IF NOT EXISTS FOR (g:Genre) REQUIRE g.key IS UNIQUE").consume();

            session.run("CREATE INDEX book_title IF NOT EXISTS FOR (b:Book) ON (b.title)").consume();
            session.run("CREATE INDEX book_pub_year IF NOT EXISTS FOR (b:Book) ON (b.pubYear)").consume();
            session.run("CREATE INDEX book_avg_rating IF NOT EXISTS FOR (b:Book) ON (b.averageRating)").consume();
            session.run("CREATE INDEX book_ratings_count IF NOT EXISTS FOR (b:Book) ON (b.ratingsCount)").consume();
            session.run("CREATE INDEX book_genre IF NOT EXISTS FOR (b:Book) ON (b.genre)").consume();
        }
        log.info("Constraints and indexes created");
    }

    private void createFullTextIndex(String database) {
        log.info("Creating full-text search index...");
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            session.run("""
                CREATE FULLTEXT INDEX bookSearch IF NOT EXISTS
                FOR (b:Book)
                ON EACH [b.title, b.titleClean, b.description, b.publisher]
                """).consume();
        }
        log.info("Full-text index created");
    }

    private void printSummary(String database) {
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            var nodeResult = session.run("MATCH (n) RETURN labels(n)[0] AS label, count(n) AS count ORDER BY count DESC");
            log.info("--- Node counts ---");
            while (nodeResult.hasNext()) {
                var record = nodeResult.next();
                log.info("  {}: {}", record.get("label").asString(), record.get("count").asLong());
            }

            var relResult = session.run("MATCH ()-[r]->() RETURN type(r) AS type, count(r) AS count ORDER BY count DESC");
            log.info("--- Relationship counts ---");
            while (relResult.hasNext()) {
                var record = relResult.next();
                log.info("  {}: {}", record.get("type").asString(), record.get("count").asLong());
            }

            var genreResult = session.run("""
                MATCH (b:Book)-[:BELONGS_TO]->(g:Genre)
                RETURN g.name AS genre, g.key AS key, count(b) AS bookCount
                ORDER BY bookCount DESC
                """);
            log.info("--- Books per genre ---");
            while (genreResult.hasNext()) {
                var record = genreResult.next();
                log.info("  {} ({}): {}", record.get("genre").asString(),
                        record.get("key").asString(), record.get("bookCount").asLong());
            }
        }
    }
}
