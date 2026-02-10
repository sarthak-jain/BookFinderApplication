import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getBooks } from '../services/api';
import BookList from '../components/BookList';
import Pagination from '../components/Pagination';
import SearchBar from '../components/SearchBar';
import './HomePage.css';

function HomePage() {
  const [books, setBooks] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    setLoading(true);
    setError(null);
    getBooks(page, 20, 'ratingsCount', 'DESC')
      .then((data) => {
        setBooks(data.content);
        setTotalPages(data.totalPages);
        setLoading(false);
      })
      .catch((err) => {
        setError('Failed to load books. Make sure the backend is running.');
        setLoading(false);
      });
  }, [page]);

  const handleSearch = (query) => {
    navigate(`/search?q=${encodeURIComponent(query)}`);
  };

  return (
    <div className="container">
      <section className="hero-section">
        <h1>Discover Your Next Favorite Book</h1>
        <p>Explore 10,000 young adult books with graph-powered recommendations</p>
        <div className="hero-search">
          <SearchBar onSearch={handleSearch} />
        </div>
      </section>

      <section className="popular-section">
        <h2>Most Popular Books</h2>
        {loading && <div className="loading-spinner">Loading books...</div>}
        {error && <div className="error-banner">{error}</div>}
        {!loading && !error && (
          <>
            <BookList books={books} />
            <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
          </>
        )}
      </section>
    </div>
  );
}

export default HomePage;
