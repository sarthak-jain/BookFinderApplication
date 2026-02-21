import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getBooks, getGenres, getMoods } from '../services/api';
import BookList from '../components/BookList';
import SearchBar from '../components/SearchBar';
import './HomePage.css';

const GENRE_ICONS = {
  young_adult: { emoji: '\uD83D\uDCDA', color: '#7B1FA2' },
  comics_graphic: { emoji: '\uD83D\uDCAC', color: '#E65100' },
  mystery_thriller_crime: { emoji: '\uD83D\uDD0D', color: '#C62828' },
  history_biography: { emoji: '\uD83C\uDFDB\uFE0F', color: '#2E7D32' },
};

function HomePage() {
  const [books, setBooks] = useState([]);
  const [genres, setGenres] = useState([]);
  const [moods, setMoods] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    setLoading(true);
    Promise.all([
      getBooks(0, 12, 'ratingsCount', 'DESC'),
      getGenres().catch(() => []),
      getMoods().catch(() => []),
    ])
      .then(([booksData, genresData, moodsData]) => {
        setBooks(booksData.content || []);
        setGenres(genresData);
        setMoods(moodsData);
        setLoading(false);
      })
      .catch(() => {
        setError('Failed to load books. Make sure the backend is running.');
        setLoading(false);
      });
  }, []);

  const handleSearch = (query) => {
    navigate(`/search?q=${encodeURIComponent(query)}`);
  };

  return (
    <div className="container">
      <section className="hero-section">
        <h1>What's Your Reading Mood Today?</h1>
        <p>Discover books across genres powered by graph-based recommendations</p>
        <div className="hero-search">
          <SearchBar onSearch={handleSearch} />
        </div>
      </section>

      {moods.length > 0 && (
        <section className="moods-section">
          <div className="section-header">
            <h2>Pick Your Mood</h2>
            <Link to="/moods" className="see-all-link">See all moods</Link>
          </div>
          <div className="mood-cards-row">
            {moods.map((mood) => (
              <Link
                key={mood.key}
                to={`/moods/${mood.key}`}
                className="mood-card"
                style={{ borderColor: mood.color }}
              >
                <div className="mood-card-dot" style={{ background: mood.color }} />
                <span className="mood-card-name">{mood.name}</span>
                <span className="mood-card-desc">{mood.description}</span>
              </Link>
            ))}
          </div>
        </section>
      )}

      {genres.length > 0 && (
        <section className="genres-section">
          <h2>Browse by Genre</h2>
          <div className="genre-cards-grid">
            {genres.map((genre) => {
              const icon = GENRE_ICONS[genre.key] || { emoji: '\uD83D\uDCD6', color: '#4A90D9' };
              return (
                <Link key={genre.key} to={`/genres/${genre.key}`} className="genre-card">
                  <span className="genre-card-icon" style={{ background: icon.color }}>
                    {icon.emoji}
                  </span>
                  <div className="genre-card-info">
                    <h3>{genre.name}</h3>
                    <span className="genre-card-count">
                      {genre.bookCount?.toLocaleString()} books
                    </span>
                  </div>
                </Link>
              );
            })}
          </div>
        </section>
      )}

      <section className="trending-section">
        <h2>Trending Across All Genres</h2>
        {loading && <div className="loading-spinner">Loading books...</div>}
        {error && <div className="error-banner">{error}</div>}
        {!loading && !error && <BookList books={books} />}
      </section>
    </div>
  );
}

export default HomePage;
