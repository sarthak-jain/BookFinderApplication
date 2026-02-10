import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { getShelfRecommendations, getShelfGraph, getAuthorGraph, getAuthorRecommendations } from '../services/api';
import BookList from '../components/BookList';
import GraphVisualization from '../components/GraphVisualization';
import './ExplorePage.css';

const POPULAR_SHELVES = [
  'young-adult', 'fantasy', 'romance', 'fiction', 'paranormal',
  'dystopia', 'science-fiction', 'mystery', 'horror', 'adventure',
  'historical-fiction', 'contemporary', 'urban-fantasy', 'vampires',
  'magic', 'teen', 'humor', 'thriller', 'action',
];

function ExplorePage() {
  const [searchParams] = useSearchParams();
  const initialShelf = searchParams.get('shelf') || '';
  const initialAuthorId = searchParams.get('authorId') || '';

  const [selectedShelf, setSelectedShelf] = useState(initialShelf || 'young-adult');
  const [authorId, setAuthorId] = useState(initialAuthorId);
  const [books, setBooks] = useState([]);
  const [graphData, setGraphData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [viewMode, setViewMode] = useState(initialAuthorId ? 'author' : 'shelf');

  useEffect(() => {
    if (initialShelf) {
      setSelectedShelf(initialShelf);
      setViewMode('shelf');
    }
    if (initialAuthorId) {
      setAuthorId(initialAuthorId);
      setViewMode('author');
    }
  }, [initialShelf, initialAuthorId]);

  useEffect(() => {
    if (viewMode === 'shelf' && selectedShelf) {
      setLoading(true);
      Promise.all([
        getShelfRecommendations(selectedShelf, 20),
        getShelfGraph(selectedShelf, 20),
      ])
        .then(([booksData, graph]) => {
          setBooks(booksData);
          setGraphData(graph);
          setLoading(false);
        })
        .catch(() => setLoading(false));
    }
  }, [selectedShelf, viewMode]);

  useEffect(() => {
    if (viewMode === 'author' && authorId) {
      setLoading(true);
      Promise.all([
        getAuthorRecommendations(authorId, 20),
        getAuthorGraph(authorId),
      ])
        .then(([booksData, graph]) => {
          setBooks(booksData);
          setGraphData(graph);
          setLoading(false);
        })
        .catch(() => setLoading(false));
    }
  }, [authorId, viewMode]);

  return (
    <div className="container explore-page">
      <h1>Explore Books</h1>

      <div className="explore-mode-toggle">
        <button
          className={viewMode === 'shelf' ? 'active' : ''}
          onClick={() => setViewMode('shelf')}
        >
          By Genre/Shelf
        </button>
        <button
          className={viewMode === 'author' ? 'active' : ''}
          onClick={() => setViewMode('author')}
        >
          By Author
        </button>
      </div>

      {viewMode === 'shelf' && (
        <div className="shelf-selector">
          {POPULAR_SHELVES.map((shelf) => (
            <button
              key={shelf}
              className={`shelf-btn ${selectedShelf === shelf ? 'active' : ''}`}
              onClick={() => setSelectedShelf(shelf)}
            >
              {shelf}
            </button>
          ))}
        </div>
      )}

      {viewMode === 'author' && (
        <div className="author-input">
          <input
            type="text"
            value={authorId}
            onChange={(e) => setAuthorId(e.target.value)}
            placeholder="Enter Author ID..."
            className="author-id-input"
          />
        </div>
      )}

      {loading && <div className="loading-spinner">Loading...</div>}

      {!loading && graphData && (
        <div className="explore-graph-section">
          <h2>Graph View</h2>
          <GraphVisualization graphData={graphData} height="450px" />
        </div>
      )}

      {!loading && books.length > 0 && (
        <div className="explore-books-section">
          <h2>
            {viewMode === 'shelf'
              ? `Top Books in "${selectedShelf}"`
              : `Books by Author ${authorId}`}
          </h2>
          <BookList books={books} />
        </div>
      )}
    </div>
  );
}

export default ExplorePage;
