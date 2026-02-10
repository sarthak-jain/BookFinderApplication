# API Reference

Base URL: `http://localhost:8080/api`

## Books

### List Books
```
GET /api/books?page=0&size=20&sortBy=ratingsCount&direction=DESC
```

**Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|------------|
| page | int | 0 | Page number (0-indexed) |
| size | int | 20 | Items per page |
| sortBy | string | ratingsCount | Sort field: `ratingsCount`, `title`, `pubYear`, `averageRating` |
| direction | string | DESC | Sort direction: `ASC` or `DESC` |

**Response:** `PaginatedResponse<BookSearchResultDTO>`

### Get Book Detail
```
GET /api/books/{bookId}
```

**Response:** `BookDTO` with authors, shelves, and series. Returns 404 if not found.

### Get Book Reviews
```
GET /api/books/{bookId}/reviews?page=0&size=10
```

**Response:** `PaginatedResponse<ReviewDTO>` ordered by helpful votes descending.

### Get Similar Books
```
GET /api/books/{bookId}/similar?limit=10
```

**Response:** `List<BookSearchResultDTO>` from SIMILAR_TO edges.

---

## Search

### Full-Text Search
```
GET /api/search?q=hunger+games&page=0&size=20&minRating=4&minYear=2000&maxYear=2020&shelves=dystopia,fantasy
```

**Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|------------|
| q | string | yes | Search query |
| page | int | no | Page number (default: 0) |
| size | int | no | Items per page (default: 20) |
| minRating | double | no | Minimum average rating filter |
| minYear | int | no | Minimum publication year |
| maxYear | int | no | Maximum publication year |
| shelves | string[] | no | Filter by shelf/genre names |

**Response:** `PaginatedResponse<BookSearchResultDTO>` with relevance scores.

### Autocomplete
```
GET /api/search/autocomplete?q=hunger&limit=5
```

**Response:** `List<BookSearchResultDTO>` — fast partial-match results.

---

## Authors

### Get Author
```
GET /api/authors/{authorId}
```

**Response:** `AuthorDTO` or 404.

### Get Author's Books
```
GET /api/authors/{authorId}/books?page=0&size=20
```

**Response:** `PaginatedResponse<BookSearchResultDTO>` ordered by ratings count.

---

## Recommendations

### Similar Books (Multi-Strategy)
```
GET /api/recommendations/similar/{bookId}?strategy=hybrid&limit=10
```

**Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|------------|
| strategy | string | hybrid | `graph`, `shelf`, `collaborative`, or `hybrid` |
| limit | int | 10 | Max results |

**Strategies:**
- `graph` — 1-hop and 2-hop SIMILAR_TO traversal
- `shelf` — Books sharing 3+ genre shelves
- `collaborative` — Users who rated this 4+ also rated other books 4+
- `hybrid` — Weighted: 0.4*graph + 0.3*shelf + 0.3*collaborative

**Response:** `List<RecommendationDTO>` with scores and strategy labels.

### Readers Also Liked
```
GET /api/recommendations/readers-also-liked/{bookId}?limit=10
```

**Response:** `List<RecommendationDTO>` from collaborative filtering.

### Top in Shelf/Genre
```
GET /api/recommendations/shelf/{shelfName}?limit=20
```

**Response:** `List<RecommendationDTO>` ordered by ratings count.

### More by Author
```
GET /api/recommendations/author/{authorId}?limit=10
```

**Response:** `List<RecommendationDTO>` of author's books.

---

## Graph Visualization

All graph endpoints return `GraphVisualizationDTO` directly consumable by vis-network.

### Book Neighborhood
```
GET /api/graph/book/{bookId}?depth=1&includeUsers=false
```

**Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|------------|
| depth | int | 1 | Traversal depth (1-3) |
| includeUsers | boolean | false | Include user nodes |

### Author Graph
```
GET /api/graph/author/{authorId}
```

### Shelf Graph
```
GET /api/graph/shelf/{shelfName}?limit=20
```

### Recommendation Graph
```
GET /api/graph/recommendations/{bookId}
```

Shows recommendation paths with color-coded edges:
- Blue: SIMILAR_TO
- Green: SHELF_SIMILAR
- Orange: READERS_ALSO_LIKED

---

## Health & Stats

### Health Check
```
GET /api/health
```

**Response:**
```json
{
  "status": "UP",
  "neo4j": "connected",
  "nodeCount": 33000
}
```

### Database Stats
```
GET /api/stats
```

**Response:**
```json
{
  "nodes": { "User": 15000, "Book": 10000, "Shelf": 1500, ... },
  "relationships": { "SHELVED_AS": 150000, "INTERACTED": 50000, ... }
}
```

---

## Data Types

### BookDTO
```json
{
  "bookId": "2767052",
  "title": "The Hunger Games (The Hunger Games, #1)",
  "titleClean": "The Hunger Games",
  "description": "In the ruins of a place once known as North America...",
  "averageRating": 4.34,
  "ratingsCount": 5470135,
  "numPages": 374,
  "publisher": "Scholastic Press",
  "pubYear": 2008,
  "imageUrl": "https://...",
  "url": "https://www.goodreads.com/book/show/2767052",
  "workId": "3706915",
  "authors": [{"authorId": "153394", "role": ""}],
  "shelves": [{"name": "dystopia", "count": 45000}],
  "seriesIds": ["73758"]
}
```

### RecommendationDTO
```json
{
  "bookId": "10572373",
  "title": "Catching Fire (The Hunger Games, #2)",
  "averageRating": 4.30,
  "ratingsCount": 2345678,
  "score": 0.85,
  "strategy": "hybrid"
}
```

### GraphVisualizationDTO
```json
{
  "nodes": [
    {"id": "book_2767052", "label": "The Hunger Games", "type": "Book", "color": "#4A90D9", "size": 30, "properties": {...}}
  ],
  "edges": [
    {"from": "book_2767052", "to": "book_10572373", "label": "SIMILAR_TO", "color": "#CCCCCC"}
  ]
}
```
