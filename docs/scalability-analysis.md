# Scalability Analysis

## Dataset Overview

The Goodreads Young Adult dataset contains:
- **93,083 books** with metadata (title, description, ratings, shelves, similar books, authors, series)
- **34.9 million interactions** (user-book: rating, is_read, date)
- **2.4 million reviews** (text, votes, comments)

## Demo Subset (Current Build)

| Metric | Count | Notes |
|--------|-------|-------|
| Book nodes | ~10,000 | Top by ratings_count |
| Author nodes | ~5,000 | Authors of selected books |
| User nodes | ~15,000 | Users with interactions in subset |
| Shelf nodes | ~1,500 | Genre tags (organizational shelves excluded) |
| Series nodes | ~1,500 | Series containing selected books |
| **Total nodes** | **~33,000** | |
| WROTE relationships | ~12,000 | Including co-authors |
| SHELVED_AS relationships | ~150,000 | Top 20 shelves per book |
| SIMILAR_TO relationships | ~30,000 | Only where both endpoints in subset |
| IN_SERIES relationships | ~5,000 | |
| INTERACTED relationships | ~50,000 | Capped |
| REVIEWED relationships | ~50,000 | Capped |
| **Total relationships** | **~297,000** | |
| **Estimated storage** | **~49 MB** | |
| **AuraDB tier** | **Free** | 200K nodes, 400K rels |

## Full Dataset Projection

| Metric | Count | Notes |
|--------|-------|-------|
| Book nodes | 93,083 | All books |
| Author nodes | ~25,000 | |
| User nodes | ~500,000 | Unique users across all interactions |
| Shelf nodes | ~15,000 | |
| Series nodes | ~10,000 | |
| **Total nodes** | **~633,000** | |
| WROTE relationships | ~120,000 | |
| SHELVED_AS relationships | ~1.5M | |
| SIMILAR_TO relationships | ~500,000 | |
| IN_SERIES relationships | ~50,000 | |
| INTERACTED relationships | ~34.9M | Full dataset |
| REVIEWED relationships | ~2.4M | Full dataset |
| **Total relationships** | **~38.7M** | |
| **Estimated storage** | **~4.5 GB** | |
| **AuraDB tier** | **Professional** | $65/month |

## Query Performance

### Current (Demo Subset)

| Query | Expected Latency | Notes |
|-------|-----------------|-------|
| Book by ID | < 5ms | Unique constraint lookup |
| Paginated book list | < 20ms | Index scan + skip/limit |
| Full-text search | < 50ms | Lucene index |
| Autocomplete | < 30ms | Wildcard full-text |
| Graph similarity (1-hop) | < 20ms | Index + traversal |
| Graph similarity (2-hop) | < 100ms | 2-hop variable-length path |
| Shelf similarity | < 100ms | Requires aggregation |
| Collaborative filtering | < 200ms | User-book-user traversal |
| Hybrid recommendation | < 500ms | 3 sub-queries combined |
| Book neighborhood graph | < 100ms | Multi-pattern MATCH |

### Full Dataset (Projected)

| Query | Expected Latency | Mitigation |
|-------|-----------------|-----------|
| Collaborative filtering | 1-5s | Add `LIMIT` on intermediate user count, index on rating |
| Hybrid recommendation | 2-8s | Cache results, run strategies in parallel |
| Shelf similarity | 500ms-2s | Pre-compute shelf similarity scores |
| Full-text search | < 100ms | Lucene scales well |

## Scaling Strategies

### Vertical Scaling
- Upgrade AuraDB tier for more memory/storage
- Neo4j query caching improves with more RAM

### Query Optimization
- **Collaborative filtering**: Add `WITH u LIMIT 100` to cap the user fanout
- **Pre-computed similarity**: Create `SIMILAR_BY_SHELF` edges offline
- **Caching**: Spring Boot `@Cacheable` on recommendation endpoints (5-minute TTL)

### Horizontal Scaling
- Neo4j Aura Enterprise supports read replicas
- Redis cache layer for frequent recommendation queries
- CDN for static frontend assets

### Data Pipeline
- Incremental loading: process new interactions/reviews since last load
- Scheduled full-text index rebuilds
- Monitoring: track query latency percentiles, node/relationship growth

## Storage Math

Neo4j storage per element (approximate):
- Node: ~128 bytes base + properties
- Relationship: ~64 bytes base + properties
- Property: ~32 bytes average (key + value)

Demo subset: `33K * 200B + 297K * 100B ≈ 36 MB` (plus indexes ≈ 49 MB total)

Full dataset: `633K * 200B + 38.7M * 100B ≈ 4 GB` (plus indexes ≈ 4.5 GB total)
