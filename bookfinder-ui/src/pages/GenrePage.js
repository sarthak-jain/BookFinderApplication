import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { getGenreBooks, getGenreTopShelves } from '../services/api';
import BookList from '../components/BookList';
import Pagination from '../components/Pagination';
import './GenrePage.css';

const GENRE_NAMES = {
  young_adult: 'Young Adult',
  comics_graphic: 'Comics & Graphic',
  mystery_thriller_crime: 'Mystery, Thriller & Crime',
  history_biography: 'History & Biography',
};

function GenrePage() {
  const { genreKey } = useParams();
  const [books, setBooks] = useState([]);
  const [shelves, setShelves] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [sortBy, setSortBy] = useState('ratingsCount');
  const [loading, setLoading] = useState(true);

  const genreName = GENRE_NAMES[genreKey] || genreKey;

  useEffect(() => {
    setPage(0);
    getGenreTopShelves(genreKey, 20).then(setShelves).catch(() => {});
  }, [genreKey]);

  useEffect(() => {
    setLoading(true);
    getGenreBooks(genreKey, page, 20, sortBy, 'DESC')
      .then((data) => {
        setBooks(data.content || []);
        setTotalPages(data.totalPages || 0);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  }, [genreKey, page, sortBy]);

  return (
    <div className="container genre-page">
      <div className="genre-header">
        <h1>{genreName}</h1>
        <div className="genre-sort">
          <label>Sort by:</label>
          <select value={sortBy} onChange={(e) => { setSortBy(e.target.value); setPage(0); }}>
            <option value="ratingsCount">Most Popular</option>
            <option value="averageRating">Highest Rated</option>
            <option value="pubYear">Newest</option>
            <option value="title">Title A-Z</option>
          </select>
        </div>
      </div>

      {shelves.length > 0 && (
        <div className="genre-shelves">
          {shelves.map((s) => (
            <span key={s.name} className="genre-shelf-chip">{s.name}</span>
          ))}
        </div>
      )}

      {loading && <div className="loading-spinner">Loading...</div>}
      {!loading && books.length > 0 && (
        <>
          <BookList books={books} />
          <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
        </>
      )}
      {!loading && books.length === 0 && (
        <p className="empty-state">No books found for this genre. Data may need to be loaded.</p>
      )}
    </div>
  );
}

export default GenrePage;
