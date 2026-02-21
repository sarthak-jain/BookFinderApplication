import React, { useState, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getGenres } from '../services/api';
import SearchBar from './SearchBar';
import './Header.css';

function Header() {
  const navigate = useNavigate();
  const [genres, setGenres] = useState([]);
  const [genreOpen, setGenreOpen] = useState(false);
  const dropdownRef = useRef(null);

  useEffect(() => {
    getGenres().then(setGenres).catch(() => {});
  }, []);

  useEffect(() => {
    const handleClick = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setGenreOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  const handleSearch = (query) => {
    navigate(`/search?q=${encodeURIComponent(query)}`);
  };

  return (
    <header className="header">
      <div className="header-inner container">
        <Link to="/" className="logo">
          <span className="logo-icon">B</span>
          <span className="logo-text">BookFinder</span>
        </Link>
        <div className="header-search">
          <SearchBar onSearch={handleSearch} />
        </div>
        <nav className="header-nav">
          <Link to="/">Home</Link>
          <div className="nav-dropdown" ref={dropdownRef}>
            <button
              className="nav-dropdown-btn"
              onClick={() => setGenreOpen(!genreOpen)}
            >
              Genres
            </button>
            {genreOpen && (
              <div className="nav-dropdown-menu">
                {genres.map((g) => (
                  <Link
                    key={g.key}
                    to={`/genres/${g.key}`}
                    onClick={() => setGenreOpen(false)}
                  >
                    {g.name}
                    <span className="genre-count">{g.bookCount?.toLocaleString()}</span>
                  </Link>
                ))}
              </div>
            )}
          </div>
          <Link to="/moods">Moods</Link>
          <Link to="/explore">Explore</Link>
        </nav>
      </div>
    </header>
  );
}

export default Header;
