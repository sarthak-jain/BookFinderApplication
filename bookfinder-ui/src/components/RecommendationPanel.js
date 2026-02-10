import React, { useState, useEffect } from 'react';
import { getRecommendations } from '../services/api';
import BookList from './BookList';
import './RecommendationPanel.css';

const STRATEGIES = [
  { key: 'hybrid', label: 'Hybrid' },
  { key: 'graph', label: 'Graph Similarity' },
  { key: 'shelf', label: 'Genre/Shelf' },
  { key: 'collaborative', label: 'Readers Also Liked' },
];

function RecommendationPanel({ bookId }) {
  const [strategy, setStrategy] = useState('hybrid');
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    getRecommendations(bookId, strategy, 12)
      .then((data) => {
        setRecommendations(data);
        setLoading(false);
      })
      .catch(() => {
        setError('Failed to load recommendations.');
        setLoading(false);
      });
  }, [bookId, strategy]);

  return (
    <div className="recommendation-panel">
      <div className="rec-strategy-tabs">
        {STRATEGIES.map((s) => (
          <button
            key={s.key}
            className={`rec-tab ${strategy === s.key ? 'active' : ''}`}
            onClick={() => setStrategy(s.key)}
          >
            {s.label}
          </button>
        ))}
      </div>

      {loading && <div className="loading-spinner">Finding recommendations...</div>}
      {error && <div className="error-banner">{error}</div>}
      {!loading && !error && (
        <BookList books={recommendations} />
      )}
    </div>
  );
}

export default RecommendationPanel;
