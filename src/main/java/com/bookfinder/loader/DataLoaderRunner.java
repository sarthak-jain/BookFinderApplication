package com.bookfinder.loader;

import com.bookfinder.config.DataLoadProperties;
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

        log.info("=== BookFinder Data Loading Pipeline ===");
        log.info("Data directory: {}", props.getDir());
        log.info("Subset size: {} books", props.getSubsetSize());
        log.info("Max interactions: {}", props.getMaxInteractions());
        log.info("Max reviews: {}", props.getMaxReviews());

        // Step 1: Create constraints and indexes
        createConstraints(database);

        // Step 2: Select top books by ratings_count
        Set<String> selectedBookIds = SubsetSelector.selectTopBookIds(
                props.getBooksPath(), props.getSubsetSize()
        );

        // Step 3: Load books (nodes + authors + series + shelves + similar)
        BookDataLoader bookLoader = new BookDataLoader(driver, database, props.getBatchSize());
        bookLoader.loadBooks(props.getBooksPath(), selectedBookIds);

        // Step 4: Load interactions
        InteractionDataLoader interactionLoader = new InteractionDataLoader(driver, database, props.getBatchSize());
        interactionLoader.loadInteractions(props.getInteractionsPath(), selectedBookIds, props.getMaxInteractions());

        // Step 5: Load reviews
        ReviewDataLoader reviewLoader = new ReviewDataLoader(driver, database, props.getBatchSize());
        reviewLoader.loadReviews(props.getReviewsPath(), selectedBookIds, props.getMaxReviews());

        // Step 6: Create full-text indexes
        createFullTextIndex(database);

        long elapsed = (System.currentTimeMillis() - start) / 1000;
        log.info("=== Data loading complete in {} seconds ===", elapsed);

        // Print summary
        printSummary(database);
    }

    private void createConstraints(String database) {
        log.info("Creating constraints and indexes...");
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            session.run("CREATE CONSTRAINT book_id IF NOT EXISTS FOR (b:Book) REQUIRE b.bookId IS UNIQUE").consume();
            session.run("CREATE CONSTRAINT author_id IF NOT EXISTS FOR (a:Author) REQUIRE a.authorId IS UNIQUE").consume();
            session.run("CREATE CONSTRAINT user_id IF NOT EXISTS FOR (u:User) REQUIRE u.userId IS UNIQUE").consume();
            session.run("CREATE CONSTRAINT shelf_name IF NOT EXISTS FOR (s:Shelf) REQUIRE s.name IS UNIQUE").consume();
            session.run("CREATE CONSTRAINT series_id IF NOT EXISTS FOR (s:Series) REQUIRE s.seriesId IS UNIQUE").consume();

            session.run("CREATE INDEX book_title IF NOT EXISTS FOR (b:Book) ON (b.title)").consume();
            session.run("CREATE INDEX book_pub_year IF NOT EXISTS FOR (b:Book) ON (b.pubYear)").consume();
            session.run("CREATE INDEX book_avg_rating IF NOT EXISTS FOR (b:Book) ON (b.averageRating)").consume();
            session.run("CREATE INDEX book_ratings_count IF NOT EXISTS FOR (b:Book) ON (b.ratingsCount)").consume();
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
        }
    }
}
