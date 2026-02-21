# BookFinder Multi-Genre & Mood-Based Discovery — Implementation Plan

## Overview

Expand BookFinder from a single-genre (Young Adult) app into a **multi-genre book discovery platform** with 4 genres and a **mood-based recommendation** system. The UI will be restructured around genre browsing and mood-driven exploration.

### Key Decisions
- **15,000 books per genre** (60K total) to fit AuraDB free tier
- **10 curated moods** + a **custom mood builder** where users pick shelf combinations
- **Genre property + Genre node** on Book nodes for both filtering and graph traversal

---

## Phase 1: Backend — Multi-Genre Data Loading

### 1.1 Restructure `DataLoadProperties` for Multi-Genre Config

**File:** `src/main/java/com/bookfinder/config/DataLoadProperties.java`

Replace single-file config with a list of genre configurations:

```java
@Component
@ConfigurationProperties(prefix = "bookfinder.data")
public class DataLoadProperties {
    private String dir = "./data";
    private String authorsFile = "metadata/goodreads_book_authors.json";
    private int batchSize = 500;
    private List<GenreConfig> genres = new ArrayList<>();

    public static class GenreConfig {
        private String name;           // e.g., "Young Adult"
        private String key;            // e.g., "young_adult"
        private String folder;         // e.g., "." (root) or "comics_graphic"
        private String booksFile;
        private String interactionsFile;
        private String reviewsFile;
        private int subsetSize = 20000;
        private int maxInteractions = 50000;
        private int maxReviews = 50000;
        // getters/setters
    }
}
```

### 1.2 Update `application.yml`

```yaml
bookfinder:
  data:
    dir: ${DATA_DIR:./data}
    authors-file: metadata/goodreads_book_authors.json
    batch-size: 500
    genres:
      - name: Young Adult
        key: young_adult
        folder: "."
        books-file: goodreads_books_young_adult.json
        interactions-file: goodreads_interactions_young_adult.json
        reviews-file: goodreads_reviews_young_adult.json
        subset-size: 15000
        max-interactions: 30000
        max-reviews: 30000
      - name: Comics & Graphic
        key: comics_graphic
        folder: comics_graphic
        books-file: goodreads_books_comics_graphic.json
        interactions-file: goodreads_interactions_comics_graphic.json
        reviews-file: goodreads_reviews_comics_graphic.json
        subset-size: 15000
        max-interactions: 30000
        max-reviews: 30000
      - name: Mystery, Thriller & Crime
        key: mystery_thriller_crime
        folder: mystery_thriller_crime
        books-file: goodreads_books_mystery_thriller_crime.json
        interactions-file: goodreads_interactions_mystery_thriller_crime.json
        reviews-file: goodreads_reviews_mystery_thriller_crime.json
        subset-size: 15000
        max-interactions: 30000
        max-reviews: 30000
      - name: History & Biography
        key: history_biography
        folder: history_biography
        books-file: goodreads_books_history_biography.json
        interactions-file: goodreads_interactions_history_biography.json
        reviews-file: goodreads_reviews_history_biography.json
        subset-size: 15000
        max-interactions: 30000
        max-reviews: 30000
```

### 1.3 Update `BookDataLoader` — Add Genre Property to Books

When loading books, set `book.genre = genreKey` on each Book node:

```cypher
MERGE (book:Book {bookId: b.bookId})
SET book.genre = $genre,
    book.title = b.title, ...
```

Also add a `Genre` node and `BELONGS_TO` relationship for graph queries:
```cypher
MERGE (g:Genre {key: $genreKey})
SET g.name = $genreName
WITH g
MATCH (book:Book {bookId: b.bookId})
MERGE (book)-[:BELONGS_TO]->(g)
```

### 1.4 New `AuthorMetadataLoader` — Load Author Names

**New file:** `src/main/java/com/bookfinder/loader/AuthorMetadataLoader.java`

Read `metadata/goodreads_book_authors.json` and update Author nodes with names:
```cypher
UNWIND $batch AS a
MATCH (author:Author {authorId: a.authorId})
SET author.name = a.name, author.averageRating = a.averageRating
```

### 1.5 Update `DataLoaderRunner` — Loop Over Genres

Instead of loading one genre, iterate over all configured genres:

```java
for (GenreConfig genre : props.getGenres()) {
    log.info("Loading genre: {}", genre.getName());
    String basePath = props.getDir() + "/" + genre.getFolder();

    Set<String> selectedIds = SubsetSelector.selectTopBookIds(
        basePath + "/" + genre.getBooksFile(), genre.getSubsetSize());

    bookLoader.loadBooks(basePath + "/" + genre.getBooksFile(),
                         selectedIds, genre.getKey(), genre.getName());
    interactionLoader.loadInteractions(...);
    reviewLoader.loadReviews(...);
}
// After all genres: load author metadata
authorMetadataLoader.loadAuthorNames(props.getAuthorsPath());
```

### 1.6 Add Neo4j Indexes for Genre

```cypher
CREATE INDEX book_genre IF NOT EXISTS FOR (b:Book) ON (b.genre)
CREATE CONSTRAINT genre_key IF NOT EXISTS FOR (g:Genre) REQUIRE g.key IS UNIQUE
```

---

## Phase 2: Backend — New API Endpoints

### 2.1 Genre Endpoints

**New controller:** `GenreController` (`/api/genres`)

| Endpoint | Description |
|----------|-------------|
| `GET /api/genres` | List all genres with book counts |
| `GET /api/genres/{genreKey}/books?page=0&size=20&sortBy=ratingsCount` | Paginated books for a genre |
| `GET /api/genres/{genreKey}/top-shelves?limit=20` | Most common shelves in a genre |

**New service:** `GenreService`

### 2.2 Mood Endpoints

**New controller:** `MoodController` (`/api/moods`)

| Endpoint | Description |
|----------|-------------|
| `GET /api/moods` | List all 10 curated moods with metadata (name, description, color, shelves) |
| `GET /api/moods/{moodKey}/books?limit=20&genre=all` | Books matching a curated mood, optionally filtered by genre |
| `POST /api/moods/custom/books` | Custom mood builder — accepts `{shelves: [...], genre?: "...", limit?: 20}`, returns matching books |

**New service:** `MoodService` with predefined mood→shelf mappings:

```java
Map<String, MoodConfig> MOODS = Map.of(
    "adventurous",    new MoodConfig("Feeling Adventurous",
        "Epic quests and thrilling journeys", "#E67E22",
        List.of("adventure", "fantasy", "action", "quest", "epic")),
    "romantic",       new MoodConfig("In the Mood for Love",
        "Heart-fluttering stories", "#E91E63",
        List.of("romance", "love", "contemporary-romance", "love-story")),
    "suspenseful",    new MoodConfig("On the Edge",
        "Can't-put-it-down suspense", "#9C27B0",
        List.of("thriller", "mystery", "suspense", "crime", "detective")),
    "feel-good",      new MoodConfig("Feel-Good Vibes",
        "Uplifting and heartwarming reads", "#4CAF50",
        List.of("humor", "funny", "feel-good", "heartwarming", "comedy")),
    "dark",           new MoodConfig("Dark & Gritty",
        "Intense and haunting stories", "#37474F",
        List.of("dark", "horror", "gothic", "dystopia", "post-apocalyptic")),
    "mind-bending",   new MoodConfig("Mind-Bending",
        "Stories that twist reality", "#2196F3",
        List.of("science-fiction", "time-travel", "dystopia", "paranormal")),
    "emotional",      new MoodConfig("Need a Good Cry",
        "Deeply moving stories", "#FF7043",
        List.of("emotional", "sad", "heartbreaking", "drama", "tear-jerker")),
    "intellectual",   new MoodConfig("Intellectually Curious",
        "Learn something new", "#795548",
        List.of("history", "biography", "non-fiction", "science", "philosophy")),
    "quick-escape",   new MoodConfig("Quick Escape",
        "Light, fast reads to unwind", "#00BCD4",
        List.of("short-stories", "novella", "contemporary", "light-read")),
    "epic-journey",   new MoodConfig("Epic Journey",
        "Grand sagas and sprawling worlds", "#FF9800",
        List.of("epic", "saga", "series", "world-building", "high-fantasy"))
);
```

**Cypher query for mood-based books:**
```cypher
MATCH (b:Book)-[r:SHELVED_AS]->(s:Shelf)
WHERE s.name IN $moodShelves
WITH b, count(s) AS shelfMatches, sum(r.count) AS totalShelfCount
WHERE shelfMatches >= 1
OPTIONAL MATCH (b)-[:BELONGS_TO]->(g:Genre)
RETURN b, g.key AS genre, shelfMatches, totalShelfCount
ORDER BY shelfMatches DESC, totalShelfCount DESC, b.ratingsCount DESC
LIMIT $limit
```

### 2.3 Update Existing Endpoints

- **`GET /api/books`** — add optional `genre` query param to filter by genre
- **`GET /api/search`** — add optional `genre` query param
- **`GET /api/stats`** — include genre breakdown in stats
- **Author endpoints** — return `author.name` (now available from metadata)

### 2.4 Update DTOs

- `BookSearchResultDTO` — add `genre` field
- `BookDTO` — add `genre` field
- `AuthorDTO` — add `name` field
- New `GenreDTO` — `key`, `name`, `bookCount`
- New `MoodDTO` — `key`, `name`, `description`, `color`, `shelfNames`

---

## Phase 3: Frontend — UI Restructure

### 3.1 New Page Layout & Navigation

**Updated routes:**

| Route | Component | Description |
|-------|-----------|-------------|
| `/` | HomePage | Hero + mood cards + genre showcase + trending |
| `/genres/:genreKey` | GenrePage (new) | Books for a specific genre with filters |
| `/moods` | MoodsPage (new) | All curated moods grid + "Build Your Own" entry |
| `/moods/custom` | CustomMoodPage (new) | Pick shelves → find matching books |
| `/moods/:moodKey` | MoodPage (new) | Books matching a curated mood |
| `/search` | SearchPage | Updated with genre filter chip |
| `/books/:bookId` | BookDetailPage | Updated with genre badge |
| `/explore` | ExplorePage | Updated with genre-aware shelves |
| `/recommendations/:bookId` | RecommendationsPage | Unchanged |

**Updated Header nav:** `Home | Genres (dropdown) | Moods | Explore | Search`

### 3.2 Redesigned Homepage

```
┌─────────────────────────────────────────────────────────┐
│  [BookFinder Logo]   [Search]   Home Genres Moods Explore│
├─────────────────────────────────────────────────────────┤
│                                                          │
│        "What's Your Reading Mood Today?"                │
│        [SearchBar]                                       │
│                                                          │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐         │
│  │Advent│ │Roman-│ │Suspe-│ │Feel- │ │Dark &│  ...     │
│  │ urous│ │ tic  │ │nsful │ │Good  │ │Gritty│         │
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘         │
│                    Mood Cards (scrollable)               │
│                                                          │
│  Browse by Genre                                        │
│  ┌─────────────┐ ┌─────────────┐                       │
│  │  Young Adult │ │Comics &     │                       │
│  │  15K books   │ │Graphic      │                       │
│  │  [Browse →]  │ │20K books    │                       │
│  └─────────────┘ └─────────────┘                       │
│  ┌─────────────┐ ┌─────────────┐                       │
│  │  Mystery,   │ │History &    │                       │
│  │  Thriller   │ │Biography    │                       │
│  │  & Crime    │ │15K books    │                       │
│  └─────────────┘ └─────────────┘                       │
│                                                          │
│  Trending Across All Genres                             │
│  [BookCard] [BookCard] [BookCard] [BookCard] ...        │
│                                                          │
├─────────────────────────────────────────────────────────┤
│  Footer                                                 │
└─────────────────────────────────────────────────────────┘
```

### 3.3 New Genre Page (`/genres/:genreKey`)

- Genre header with name, description, book count
- Sort options: Most Popular, Highest Rated, Newest
- Genre-specific shelf filter chips (top shelves for that genre)
- Paginated book grid
- Genre graph visualization (optional toggle)

### 3.4 New Mood Page (`/moods/:moodKey`)

- Mood header with name, description, color accent
- Shows which shelves define this mood
- Book results across all genres (with genre badge on each card)
- Optional genre filter to narrow results
- "Try another mood" section with other mood cards

### 3.5 Custom Mood Builder (`/moods/custom`)

```
┌─────────────────────────────────────────────────────────┐
│  Build Your Reading Mood                                │
│                                                          │
│  Select shelves that match your mood:                   │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐          │
│  │fantasy ✓│ │romance │ │mystery │ │humor  ✓│  ...     │
│  └────────┘ └────────┘ └────────┘ └────────┘          │
│  (Multi-select shelf chips, loaded from /api/genres     │
│   top-shelves, grouped or searchable)                   │
│                                                          │
│  [Optional] Filter by genre:                            │
│  ( ) All  (•) Young Adult  ( ) Comics  ( ) Mystery ...  │
│                                                          │
│  [Find Books →]                                         │
│                                                          │
│  Results: 42 books match your mood                      │
│  [BookCard] [BookCard] [BookCard] ...                   │
└─────────────────────────────────────────────────────────┘
```

- Shelves populated from the top shelves across all genres (via API)
- Multi-select chip interface for picking shelves
- Optional genre filter radio buttons
- Calls `POST /api/moods/custom/books` with selected shelves
- Results show genre badge on each book card

### 3.6 Updated Components

**BookCard** — Add genre badge (small colored chip):
```
┌─────────────┐
│  [Cover]    │
│  Title      │
│  ★★★★☆ 4.2 │
│  2014       │
│  [YA badge] │
└─────────────┘
```

**Header** — Add Genres dropdown and Moods link in nav

**SearchPage** — Add genre filter chips above results

**BookDetailPage** — Show genre badge next to title, author names instead of IDs

**ExplorePage** — Add genre selector at top, shelves update per genre

### 3.7 New API Service Methods

```javascript
// api.js additions
getGenres()                              // GET /api/genres
getGenreBooks(genreKey, page, size, sort) // GET /api/genres/{key}/books
getGenreTopShelves(genreKey, limit)       // GET /api/genres/{key}/top-shelves
getMoods()                                // GET /api/moods
getMoodBooks(moodKey, limit, genre)       // GET /api/moods/{key}/books
getCustomMoodBooks(shelves, genre, limit) // POST /api/moods/custom/books
getTopShelves(limit)                     // GET /api/genres/all/top-shelves (for mood builder)
```

---

## Phase 4: Update Design Documentation

### 4.1 Update `docs/design-decisions.md`
- Add decision: "Why Multi-Genre with Genre Property + Genre Node?"
- Add decision: "Why Mood-Based Discovery via Shelf Mapping?"
- Update the subset strategy section for 15K per genre (60K total)

### 4.2 Update `docs/scalability-analysis.md`
- Update node/relationship estimates for 4 genres × 15K books
- New projected totals: ~129K nodes, ~376K relationships
- Confirm AuraDB free tier compatibility

### 4.3 Update `docs/api-reference.md`
- Add Genre endpoints
- Add Mood endpoints
- Update existing endpoint docs with new `genre` parameter

---

## AuraDB Free Tier Budget (15K books × 4 genres)

**Limits:** ~200K nodes, ~400K relationships

**Estimated totals for 4 × 15K books:**

| Node Type | Estimate |
|-----------|----------|
| Book | 60,000 |
| Author | ~15,000 |
| User | ~45,000 (capped by 30K interactions + 30K reviews per genre) |
| Shelf | ~4,000 |
| Series | ~5,000 |
| Genre | 4 |
| **Total nodes** | **~129,000** |

| Relationship Type | Estimate |
|-------------------|----------|
| WROTE | ~36,000 |
| SHELVED_AS | ~180,000 (top 15 shelves per book) |
| SIMILAR_TO | ~45,000 |
| IN_SERIES | ~15,000 |
| BELONGS_TO | ~60,000 |
| INTERACTED | ~120,000 |
| REVIEWED | ~120,000 |
| **Total rels** | **~376,000** |

**Verdict:** Safely within free tier limits (129K/200K nodes, 376K/400K rels).
If tight, first lever to pull is reducing SHELVED_AS (top 12 shelves per book) or INTERACTED/REVIEWED caps.

---

## Implementation Order

1. **Phase 1** — Backend data loading (multi-genre + author metadata)
2. **Phase 2** — New API endpoints (genres, moods, updated search)
3. **Phase 3** — Frontend UI restructure
4. **Phase 4** — Documentation updates

Each phase is independently testable. Phase 1 requires re-loading data into Neo4j (clearing existing data first).
