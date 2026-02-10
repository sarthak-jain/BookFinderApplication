# Design Decisions

## Why Neo4j?

**Decision**: Use Neo4j graph database instead of a relational database (PostgreSQL/MySQL).

**Rationale**:
- Book recommendations are inherently graph problems — "users who liked X also liked Y" is a graph traversal
- SIMILAR_TO, SHELVED_AS, and INTERACTED relationships are natural graph edges
- Multi-hop traversals (2-hop similarity, collaborative filtering) are O(1) per hop in Neo4j vs. expensive JOINs in SQL
- Full-text search is built into Neo4j 5.x via Lucene-backed indexes, eliminating the need for a separate search engine
- AuraDB free tier provides sufficient capacity for the demo dataset

**Tradeoffs**:
- Neo4j has higher operational complexity than a simple PostgreSQL setup
- AuraDB free tier has storage limits (~200K nodes, ~400K relationships)
- Graph databases are less familiar to many developers

## Why Neo4j Full-Text Search over Elasticsearch/Solr?

**Decision**: Use Neo4j's built-in full-text indexes instead of a dedicated search engine.

**Rationale**:
- Eliminates an additional infrastructure dependency
- Full-text index on `Book(title, titleClean, description, publisher)` provides good search quality for our dataset size
- Supports Lucene query syntax (wildcards, fuzzy matching) out of the box
- Search results can be directly combined with graph traversals in a single Cypher query

**Tradeoffs**:
- Neo4j full-text search lacks advanced features like faceting, highlighting, and sophisticated relevance tuning
- For a production system with millions of books, a dedicated search engine would be better

## Why vis-network for Graph Visualization?

**Decision**: Use vis-network instead of D3.js, Cytoscape.js, or Sigma.js.

**Rationale**:
- vis-network has excellent physics-based layout (forceAtlas2) out of the box
- Simpler API than D3.js for interactive graph visualization
- Built-in features: zoom, pan, hover tooltips, click events, navigation buttons
- Good performance for graphs up to ~500 nodes (sufficient for our use case)
- npm package with straightforward React integration

**Tradeoffs**:
- Less customizable than D3.js
- Performance degrades with very large graphs (1000+ nodes)
- vis-network is less actively maintained than some alternatives

## Why Spring Boot + Spring Data Neo4j?

**Decision**: Java/Spring Boot backend with Spring Data Neo4j.

**Rationale**:
- Spring Data Neo4j provides seamless ORM-like mapping for Neo4j entities
- Spring Boot auto-configuration handles Neo4j driver setup, connection pooling
- Cypher DSL support for type-safe queries
- Strong ecosystem for REST API development (Jackson, validation, testing)
- Portfolio value: demonstrates full-stack Java development

**Tradeoffs**:
- Heavier than a Python/FastAPI or Node.js/Express alternative
- JVM startup time is slower (mitigated by Spring Boot 3.x improvements)

## Why Top-10K Subset by ratings_count?

**Decision**: Select the top 10,000 books by `ratings_count` descending for the demo dataset.

**Rationale**:
- High-engagement books have more interactions, reviews, and SIMILAR_TO edges — resulting in a denser, more useful graph
- 10K books fits comfortably within AuraDB free tier limits
- Ensures collaborative filtering has enough user overlap to produce meaningful recommendations
- Popular books are more recognizable, making demos more relatable

**Tradeoffs**:
- Excludes niche/lesser-known books that might benefit from recommendation
- The subset is biased toward popular titles

## Why Hybrid Recommendation Strategy?

**Decision**: Default recommendation strategy blends graph (0.4), shelf (0.3), and collaborative (0.3) scores.

**Rationale**:
- No single strategy excels in all cases — graph similarity misses genre connections, collaborative filtering has cold-start issues
- Weighted hybrid provides more diverse recommendations
- Weights chosen empirically: graph similarity has highest signal quality, shelf and collaborative add breadth

**Tradeoffs**:
- Harder to explain to users why a specific book was recommended
- Three separate Neo4j queries per recommendation request (acceptable for demo scale)

## Data Loading: Batch UNWIND vs. Individual Writes

**Decision**: Use Cypher `UNWIND $batch` with batch sizes of 500 for all data loading.

**Rationale**:
- Reduces Neo4j round-trips by 500x compared to individual CREATE statements
- Network latency to AuraDB makes individual writes prohibitively slow
- 500 is a good balance between memory usage and batch efficiency
- Stream-based processing with `JsonLineReader` keeps JVM memory usage low even for 12GB files

**Tradeoffs**:
- More complex code than simple `session.save()` calls
- Error handling is coarser (entire batch fails if one record is malformed)
