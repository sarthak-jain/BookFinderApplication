import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { autocomplete } from '../services/api';
import './SearchBar.css';

function SearchBar({ onSearch, initialQuery = '' }) {
  const [query, setQuery] = useState(initialQuery);
  const [suggestions, setSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const debounceRef = useRef(null);
  const wrapperRef = useRef(null);
  const navigate = useNavigate();

  useEffect(() => {
    setQuery(initialQuery);
  }, [initialQuery]);

  useEffect(() => {
    function handleClickOutside(e) {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target)) {
        setShowSuggestions(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleChange = (e) => {
    const value = e.target.value;
    setQuery(value);

    if (debounceRef.current) clearTimeout(debounceRef.current);

    if (value.trim().length >= 2) {
      debounceRef.current = setTimeout(async () => {
        try {
          const results = await autocomplete(value.trim());
          setSuggestions(results);
          setShowSuggestions(true);
        } catch {
          setSuggestions([]);
        }
      }, 300);
    } else {
      setSuggestions([]);
      setShowSuggestions(false);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (query.trim()) {
      setShowSuggestions(false);
      if (onSearch) {
        onSearch(query.trim());
      }
    }
  };

  const handleSuggestionClick = (book) => {
    setShowSuggestions(false);
    setQuery(book.title || '');
    navigate(`/books/${book.bookId}`);
  };

  return (
    <div className="search-bar-wrapper" ref={wrapperRef}>
      <form onSubmit={handleSubmit} className="search-bar">
        <input
          type="text"
          value={query}
          onChange={handleChange}
          placeholder="Search books by title, author, or description..."
          className="search-input"
        />
        <button type="submit" className="search-button">Search</button>
      </form>
      {showSuggestions && suggestions.length > 0 && (
        <ul className="search-suggestions">
          {suggestions.map((book) => (
            <li key={book.bookId} onClick={() => handleSuggestionClick(book)}>
              <div className="suggestion-title">{book.title}</div>
              <div className="suggestion-meta">
                {book.averageRating > 0 && <span>{book.averageRating.toFixed(1)} stars</span>}
                {book.pubYear > 0 && <span>{book.pubYear}</span>}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default SearchBar;
