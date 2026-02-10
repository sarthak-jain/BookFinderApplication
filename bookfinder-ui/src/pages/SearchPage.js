import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { search } from '../services/api';
import BookList from '../components/BookList';
import Pagination from '../components/Pagination';
import SearchFilters from '../components/SearchFilters';
import './SearchPage.css';

function SearchPage() {
  const [searchParams] = useSearchParams();
  const query = searchParams.get('q') || '';
  const [results, setResults] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [filters, setFilters] = useState({});

  useEffect(() => {
    setPage(0);
  }, [query]);

  useEffect(() => {
    if (!query) return;
    setLoading(true);
    setError(null);
    search(query, page, 20, filters)
      .then((data) => {
        setResults(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
        setLoading(false);
      })
      .catch(() => {
        setError('Search failed. Please try again.');
        setLoading(false);
      });
  }, [query, page, filters]);

  const handleFilterChange = (newFilters) => {
    setFilters(newFilters);
    setPage(0);
  };

  return (
    <div className="container search-page">
      <div className="search-page-sidebar">
        <SearchFilters onFilterChange={handleFilterChange} filters={filters} />
      </div>
      <div className="search-page-main">
        <div className="search-page-header">
          <h2>Search Results for "{query}"</h2>
          {totalElements > 0 && (
            <p className="search-count">{totalElements} books found</p>
          )}
        </div>
        {loading && <div className="loading-spinner">Searching...</div>}
        {error && <div className="error-banner">{error}</div>}
        {!loading && !error && (
          <>
            <BookList books={results} />
            <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
          </>
        )}
      </div>
    </div>
  );
}

export default SearchPage;
