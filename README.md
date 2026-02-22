# BookFinder - Book Recommendation Engine

A full-stack book recommendation engine powered by **Neo4j graph database**, **Spring Boot**, and **React**. Uses Goodreads datasets across 4 genres (60K books, 120K+ interactions) to deliver graph-powered recommendations, mood-based discovery, and interactive graph visualization.

**Live Demo:** [https://sarthak-jain.github.io/BookFinderApplication/](https://sarthak-jain.github.io/BookFinderApplication/)

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 21, Spring Boot 3.2, Spring Data Neo4j |
| **Database** | Neo4j (AuraDB free tier compatible) |
| **Search** | Neo4j full-text indexes (Lucene-based) |
| **Frontend** | React 18, React Router, vis-network |
| **Build** | Maven, npm |

## Features

- **Multi-Genre Library** — 4 genres: Young Adult, Comics & Graphic, Mystery/Thriller/Crime, History & Biography (15K books each)
- **Mood-Based Discovery** — 10 curated moods + custom mood builder using shelf combinations
- **Full-Text Search** with autocomplete, filters (rating, year range, genre)
- **4 Recommendation Strategies**:
  - **Graph Similarity** — 1-hop and 2-hop SIMILAR_TO traversal
  - **Genre/Shelf Similarity** — books sharing 3+ genre shelves
  - **Collaborative Filtering** — users who rated this book highly also rated...
  - **Hybrid** — weighted blend (0.4 graph + 0.3 shelf + 0.3 collaborative)
- **Interactive Graph Visualization** — vis-network with color-coded nodes, click-to-navigate
- **Book Detail Pages** — reviews, ratings, shelves, author links
- **Browse by Genre** — genre pages with top shelves and graph view

## Architecture

```
React Frontend (GitHub Pages)
        |
        v
Spring Boot REST API (Railway)
   /api/books, /api/search, /api/recommendations, /api/graph, /api/genres, /api/moods
        |
        v
Neo4j Graph Database (AuraDB)
   Book, Author, User, Shelf, Series, Genre nodes
   WROTE, SIMILAR_TO, SHELVED_AS, INTERACTED, REVIEWED, BELONGS_TO relationships
```

## Neo4j Graph Schema

**Nodes:** `Book`, `Author`, `User`, `Shelf`, `Series`, `Genre`

**Relationships:**
- `(:Author)-[:WROTE]->(:Book)` — authorship (with role)
- `(:Book)-[:IN_SERIES]->(:Series)` — series membership
- `(:Book)-[:SHELVED_AS]->(:Shelf)` — genre tags (with count)
- `(:Book)-[:SIMILAR_TO]->(:Book)` — Goodreads similarity
- `(:User)-[:INTERACTED]->(:Book)` — reading activity (rating, isRead)
- `(:User)-[:REVIEWED]->(:Book)` — review text, votes, comments

## Quick Start

### Prerequisites

- Java 21 (JDK)
- Maven 3.9+
- Node.js 18+ and npm
- Neo4j database (AuraDB free tier or local instance)

### 1. Configure Neo4j Connection

Set environment variables:

```bash
export NEO4J_URI=neo4j+s://your-instance.databases.neo4j.io
export NEO4J_USERNAME=neo4j
export NEO4J_PASSWORD=your-password
```

Or edit `src/main/resources/application.yml` directly.

### 2. Load Data

Place the Goodreads dataset files in `data/` with genre subfolders:
- `data/goodreads_books_young_adult.json` (Young Adult)
- `data/comics_graphic/` (Comics & Graphic)
- `data/mystery_thriller_crime/` (Mystery, Thriller & Crime)
- `data/history_biography/` (History & Biography)
- `data/goodreads_book_authors.json/goodreads_book_authors.json` (Author metadata)

Run the data loader:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=load-data
```

This selects the top 15K books per genre by ratings_count and loads interactions and reviews for all 4 genres.

### 3. Start Backend

```bash
mvn spring-boot:run
```

Backend runs at `http://localhost:8080`.

### 4. Start Frontend

```bash
cd bookfinder-ui
npm install
npm start
```

Frontend runs at `http://localhost:3000` with proxy to backend.

### 5. Deployment

The app is deployed via GitHub:
- **Frontend:** GitHub Pages (auto-deploys on push via GitHub Actions)
- **Backend:** Railway (auto-deploys from GitHub repo via Dockerfile)
- **Database:** Neo4j AuraDB (free tier)

## API Reference

| Endpoint | Description |
|----------|------------|
| `GET /api/books?page=0&size=20&sortBy=ratingsCount&direction=DESC` | Paginated book listing |
| `GET /api/books/{bookId}` | Book detail with authors, shelves, series |
| `GET /api/books/{bookId}/reviews?page=0&size=10` | Paginated reviews |
| `GET /api/books/{bookId}/similar?limit=10` | Similar books |
| `GET /api/search?q=hunger+games&minRating=4&minYear=2000` | Full-text search with filters |
| `GET /api/search/autocomplete?q=hunger&limit=5` | Fast autocomplete |
| `GET /api/authors/{authorId}` | Author detail |
| `GET /api/authors/{authorId}/books` | Books by author |
| `GET /api/recommendations/similar/{bookId}?strategy=hybrid` | Recommendations (graph/shelf/collaborative/hybrid) |
| `GET /api/recommendations/readers-also-liked/{bookId}` | Collaborative filtering |
| `GET /api/recommendations/shelf/{shelfName}` | Top books in genre |
| `GET /api/recommendations/author/{authorId}` | More by author |
| `GET /api/graph/book/{bookId}?depth=1&includeUsers=false` | Book neighborhood graph |
| `GET /api/graph/author/{authorId}` | Author-centered graph |
| `GET /api/graph/shelf/{shelfName}` | Shelf-centered graph |
| `GET /api/graph/recommendations/{bookId}` | Recommendation paths graph |
| `GET /api/genres` | List genres with book counts |
| `GET /api/genres/{key}/books` | Paginated genre books |
| `GET /api/genres/{key}/top-shelves` | Genre shelf breakdown |
| `GET /api/moods` | 10 curated moods |
| `GET /api/moods/{key}/books` | Mood-matched books |
| `POST /api/moods/custom/books` | Custom mood builder |
| `GET /api/health` | Health check with Neo4j status |
| `GET /api/stats` | Node and relationship counts |

## Scalability

| Scale | Nodes | Relationships | Storage | AuraDB Tier |
|-------|-------|--------------|---------|-------------|
| Demo (4 genres) | ~129K | ~376K | ~70 MB | Free |
| Full dataset (4 genres) | ~2.5M | ~100M+ | ~15 GB | Professional |

## Documentation

- [Design Decisions](docs/design-decisions.md)
- [Scalability Analysis](docs/scalability-analysis.md)
- [API Reference](docs/api-reference.md)

## Data Source

[Goodreads Book Graph Datasets](https://mengtingwan.github.io/data/goodreads.html) — Young Adult, Comics & Graphic, Mystery/Thriller/Crime, and History & Biography genre subsets.
