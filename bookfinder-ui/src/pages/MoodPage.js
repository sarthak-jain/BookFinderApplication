import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getMood, getMoodBooks, getGenres } from '../services/api';
import BookList from '../components/BookList';
import './MoodPage.css';

function MoodPage() {
  const { moodKey } = useParams();
  const [mood, setMood] = useState(null);
  const [books, setBooks] = useState([]);
  const [genres, setGenres] = useState([]);
  const [selectedGenre, setSelectedGenre] = useState('all');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getMood(moodKey).then(setMood).catch(() => {});
    getGenres().then(setGenres).catch(() => {});
  }, [moodKey]);

  useEffect(() => {
    setLoading(true);
    getMoodBooks(moodKey, 30, selectedGenre)
      .then((data) => {
        setBooks(data);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  }, [moodKey, selectedGenre]);

  if (!mood && !loading) {
    return <div className="container"><p>Mood not found.</p></div>;
  }

  return (
    <div className="container mood-page">
      {mood && (
        <div className="mood-page-header" style={{ borderColor: mood.color }}>
          <div className="mood-dot" style={{ background: mood.color }} />
          <div>
            <h1>{mood.name}</h1>
            <p>{mood.description}</p>
            <div className="mood-page-shelves">
              {mood.shelves?.map((s) => (
                <span key={s} className="mood-page-shelf-tag">{s}</span>
              ))}
            </div>
          </div>
        </div>
      )}

      <div className="mood-genre-filter">
        <span className="filter-label">Filter by genre:</span>
        <button
          className={selectedGenre === 'all' ? 'active' : ''}
          onClick={() => setSelectedGenre('all')}
        >All</button>
        {genres.map((g) => (
          <button
            key={g.key}
            className={selectedGenre === g.key ? 'active' : ''}
            onClick={() => setSelectedGenre(g.key)}
          >{g.name}</button>
        ))}
      </div>

      {loading && <div className="loading-spinner">Loading books...</div>}
      {!loading && books.length > 0 && <BookList books={books} />}
      {!loading && books.length === 0 && (
        <p className="empty-state">No books match this mood yet. Try a different genre filter.</p>
      )}

      <div className="mood-page-other">
        <h3>Try Another Mood</h3>
        <Link to="/moods" className="try-moods-link">Browse all moods</Link>
      </div>
    </div>
  );
}

export default MoodPage;
