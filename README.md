# BookFinder - Book Recommendation Engine

A full-stack book recommendation engine powered by **Neo4j graph database**, **Spring Boot**, and **React**. Uses the Goodreads Young Adult dataset (93K books, 34.9M interactions) to deliver graph-powered recommendations through four distinct strategies.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 21, Spring Boot 3.2, Spring Data Neo4j |
| **Database** | Neo4j (AuraDB free tier compatible) |
| **Search** | Neo4j full-text indexes (Lucene-based) |
| **Frontend** | React 18, React Router, vis-network |
| **Build** | Maven, npm |

## Features

- **Full-Text Search** with autocomplete, filters (rating, year range, genre)
- **4 Recommendation Strategies**:
  - **Graph Similarity** — 1-hop and 2-hop SIMILAR_TO traversal
  - **Genre/Shelf Similarity** — books sharing 3+ genre shelves
  - **Collaborative Filtering** — users who rated this book highly also rated...
  - **Hybrid** — weighted blend (0.4 graph + 0.3 shelf + 0.3 collaborative)
- **Interactive Graph Visualization** — vis-network with color-coded nodes, click-to-navigate
- **Book Detail Pages** — reviews, ratings, shelves, author links
- **Explore by Genre** — browse top books per shelf with graph view

## Architecture

```
React Frontend (localhost:3000)
        |
        v
Spring Boot REST API (localhost:8080)
   /api/books, /api/search, /api/recommendations, /api/graph
        |
        v
Neo4j Graph Database (AuraDB or local)
   Book, Author, User, Shelf, Series nodes
   WROTE, SIMILAR_TO, SHELVED_AS, INTERACTED, REVIEWED relationships
```

## Neo4j Graph Schema

**Nodes:** `Book`, `Author`, `User`, `Shelf`, `Series`

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

Place the Goodreads Young Adult dataset files in `data/`:
- `goodreads_books_young_adult.json`
- `goodreads_interactions_young_adult.json`
- `goodreads_reviews_young_adult.json`

Run the data loader:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=load-data
```

This selects the top 10K books by ratings_count and loads ~50K interactions and ~50K reviews. Takes 2-5 minutes depending on network speed.

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

### 5. Production Build

```bash
cd bookfinder-ui
npm run build
# Copy build output to Spring Boot static resources:
cp -r build/* ../src/main/resources/static/
# Restart backend — it now serves the SPA at localhost:8080
```

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
| `GET /api/health` | Health check with Neo4j status |
| `GET /api/stats` | Node and relationship counts |

## Scalability

| Scale | Nodes | Relationships | Storage | AuraDB Tier |
|-------|-------|--------------|---------|-------------|
| Demo (this build) | ~33K | ~342K | ~49 MB | Free |
| Full YA dataset | ~633K | ~38.7M | ~4.5 GB | Professional |

## Documentation

- [Design Decisions](docs/design-decisions.md)
- [Scalability Analysis](docs/scalability-analysis.md)
- [API Reference](docs/api-reference.md)

## Data Source

[Goodreads Book Graph Datasets](https://mengtingwan.github.io/data/goodreads.html) — Young Adult genre subset.
