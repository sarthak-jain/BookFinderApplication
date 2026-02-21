import React, { useState, useEffect } from 'react';
import { getAllTopShelves, getCustomMoodBooks, getGenres } from '../services/api';
import BookList from '../components/BookList';
import './CustomMoodPage.css';

function CustomMoodPage() {
  const [shelves, setShelves] = useState([]);
  const [genres, setGenres] = useState([]);
  const [selectedShelves, setSelectedShelves] = useState([]);
  const [selectedGenre, setSelectedGenre] = useState('all');
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  useEffect(() => {
    getAllTopShelves(60).then(setShelves).catch(() => {});
    getGenres().then(setGenres).catch(() => {});
  }, []);

  const toggleShelf = (name) => {
    setSelectedShelves((prev) =>
      prev.includes(name) ? prev.filter((s) => s !== name) : [...prev, name]
    );
  };

  const handleSearch = () => {
    if (selectedShelves.length === 0) return;
    setLoading(true);
    setSearched(true);
    const genre = selectedGenre === 'all' ? null : selectedGenre;
    getCustomMoodBooks(selectedShelves, genre, 30)
      .then((data) => {
        setBooks(data);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  };

  return (
    <div className="container custom-mood-page">
      <h1>Build Your Reading Mood</h1>
      <p className="custom-mood-subtitle">
        Select shelves that match your mood, then discover matching books.
      </p>

      <div className="custom-mood-shelves">
        <h3>Select shelves ({selectedShelves.length} selected)</h3>
        <div className="shelf-chips">
          {shelves.map((s) => (
            <button
              key={s.name}
              className={`shelf-chip ${selectedShelves.includes(s.name) ? 'selected' : ''}`}
              onClick={() => toggleShelf(s.name)}
            >
              {s.name}
            </button>
          ))}
        </div>
      </div>

      <div className="custom-mood-genre-filter">
        <span className="filter-label">Filter by genre (optional):</span>
        <div className="genre-radio-group">
          <button
            className={selectedGenre === 'all' ? 'active' : ''}
            onClick={() => setSelectedGenre('all')}
          >All Genres</button>
          {genres.map((g) => (
            <button
              key={g.key}
              className={selectedGenre === g.key ? 'active' : ''}
              onClick={() => setSelectedGenre(g.key)}
            >{g.name}</button>
          ))}
        </div>
      </div>

      <button
        className="find-books-btn"
        onClick={handleSearch}
        disabled={selectedShelves.length === 0}
      >
        Find Books
      </button>

      {loading && <div className="loading-spinner">Searching...</div>}

      {!loading && searched && (
        <div className="custom-mood-results">
          <h2>{books.length} books match your mood</h2>
          {books.length > 0 ? (
            <BookList books={books} />
          ) : (
            <p className="empty-state">No books match your selection. Try different shelves.</p>
          )}
        </div>
      )}
    </div>
  );
}

export default CustomMoodPage;
