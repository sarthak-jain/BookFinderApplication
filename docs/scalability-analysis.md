# Scalability Analysis

## Dataset Overview

The BookFinder application loads data from 4 Goodreads genre datasets:

| Genre | Books File | Interactions File | Reviews File |
|-------|-----------|-------------------|-------------|
| Young Adult | 469 MB (~93K books) | 12 GB (~34.9M) | 2.6 GB (~2.4M) |
| Comics & Graphic | 338 MB | 2.4 GB | 449 MB |
| Mystery, Thriller & Crime | 1.1 GB | 7.9 GB | 1.8 GB |
| History & Biography | 1.4 GB | 10 GB | 2.1 GB |

**Author metadata:** 102 MB (`goodreads_book_authors.json`)

## Multi-Genre Demo Build (Current)

15,000 books per genre, 60,000 total.

| Metric | Count | Notes |
|--------|-------|-------|
| Book nodes | ~60,000 | Top 15K per genre by ratings_count |
| Author nodes | ~15,000 | Authors of selected books |
| User nodes | ~45,000 | Users with interactions in subset |
| Shelf nodes | ~4,000 | Genre tags (organizational shelves excluded) |
| Series nodes | ~5,000 | Series containing selected books |
| Genre nodes | 4 | Young Adult, Comics, Mystery, History |
| **Total nodes** | **~129,000** | |
| WROTE relationships | ~36,000 | Including co-authors |
| SHELVED_AS relationships | ~180,000 | Top 15 shelves per book |
| SIMILAR_TO relationships | ~45,000 | Only where both endpoints in subset |
| IN_SERIES relationships | ~15,000 | |
| BELONGS_TO relationships | ~60,000 | Book → Genre |
| INTERACTED relationships | ~120,000 | 30K per genre |
| REVIEWED relationships | ~120,000 | 30K per genre |
| **Total relationships** | **~376,000** | |
| **Estimated storage** | **~70 MB** | |
| **AuraDB tier** | **Free** | 200K nodes, 400K rels |

### AuraDB Free Tier Budget

| Resource | Used | Limit | Utilization |
|----------|------|-------|------------|
| Nodes | ~129K | 200K | 65% |
| Relationships | ~376K | 400K | 94% |

**First levers to pull if tight:**
- Reduce SHELVED_AS (top 12 shelves per book instead of 15)
- Reduce max-interactions per genre (25K instead of 30K)

## Full Dataset Projection (All 4 Genres)

| Metric | Count | Notes |
|--------|-------|-------|
| Book nodes | ~400,000 | All books across 4 genres |
| Author nodes | ~80,000 | |
| User nodes | ~2,000,000 | Unique users across all interactions |
| Shelf nodes | ~30,000 | |
| Series nodes | ~20,000 | |
| Genre nodes | 4 | |
| **Total nodes** | **~2,530,000** | |
| **Total relationships** | **~100M+** | |
| **Estimated storage** | **~15 GB** | |
| **AuraDB tier** | **Professional** | $65/month |

## Query Performance

### Current (Multi-Genre Demo)

| Query | Expected Latency | Notes |
|-------|-----------------|-------|
| Book by ID | < 5ms | Unique constraint lookup |
| Paginated book list | < 20ms | Index scan + skip/limit |
| Paginated book list (genre filtered) | < 20ms | Genre index + skip/limit |
| Full-text search | < 50ms | Lucene index |
| Full-text search (genre filtered) | < 60ms | Lucene + genre property filter |
| Autocomplete | < 30ms | Wildcard full-text |
| Graph similarity (1-hop) | < 20ms | Index + traversal |
| Graph similarity (2-hop) | < 100ms | 2-hop variable-length path |
| Shelf similarity | < 150ms | Larger graph, more shelves |
| Collaborative filtering | < 300ms | Cross-genre user overlap possible |
| Hybrid recommendation | < 600ms | 3 sub-queries combined |
| Mood-based discovery | < 200ms | Shelf matching with aggregation |
| Custom mood (multi-shelf) | < 300ms | Variable shelf count |
| Genre listing | < 20ms | Small aggregation (4 genres) |
| Genre top shelves | < 50ms | Aggregation with genre filter |

### Full Dataset (Projected)

| Query | Expected Latency | Mitigation |
|-------|-----------------|-----------|
| Collaborative filtering | 1-5s | `LIMIT` on intermediate users, rating index |
| Hybrid recommendation | 2-8s | Cache results, parallel strategies |
| Shelf similarity | 500ms-2s | Pre-compute shelf similarity scores |
| Mood-based discovery | 500ms-1s | Pre-compute mood scores or add caching |
| Full-text search | < 100ms | Lucene scales well |

## Scaling Strategies

### Vertical Scaling
- Upgrade AuraDB tier for more memory/storage
- Neo4j query caching improves with more RAM

### Query Optimization
- **Collaborative filtering**: Add `WITH u LIMIT 100` to cap the user fanout
- **Pre-computed similarity**: Create `SIMILAR_BY_SHELF` edges offline
- **Caching**: Spring Boot `@Cacheable` on recommendation and mood endpoints (5-minute TTL)
- **Mood pre-computation**: Materialize mood scores on Book nodes for fastest queries

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

Demo (4 genres): `129K * 200B + 376K * 100B ≈ 63 MB` (plus indexes ≈ 70 MB total)

Full dataset: `2.5M * 200B + 100M * 100B ≈ 10.5 GB` (plus indexes ≈ 15 GB total)
