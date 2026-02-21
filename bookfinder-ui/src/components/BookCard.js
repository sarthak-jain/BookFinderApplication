import React from 'react';
import { Link } from 'react-router-dom';
import StarRating from './StarRating';
import './BookCard.css';

const GENRE_COLORS = {
  young_adult: { bg: '#EDE7F6', text: '#7B1FA2', label: 'Young Adult' },
  comics_graphic: { bg: '#FFF3E0', text: '#E65100', label: 'Comics' },
  mystery_thriller_crime: { bg: '#FCE4EC', text: '#C62828', label: 'Mystery' },
  history_biography: { bg: '#E8F5E9', text: '#2E7D32', label: 'History' },
};

function BookCard({ book }) {
  const hasImage = book.imageUrl && !book.imageUrl.includes('nophoto');
  const genreInfo = book.genre ? GENRE_COLORS[book.genre] : null;

  return (
    <div className="book-card">
      <Link to={`/books/${book.bookId}`} className="book-card-link">
        <div className="book-card-cover">
          {hasImage ? (
            <img src={book.imageUrl} alt={book.title} loading="lazy" />
          ) : (
            <div className="book-card-placeholder">
              <span>{(book.titleClean || book.title || '?').charAt(0)}</span>
            </div>
          )}
        </div>
        <div className="book-card-info">
          <h3 className="book-card-title">{book.titleClean || book.title}</h3>
          <div className="book-card-meta">
            {book.averageRating > 0 && (
              <div className="book-card-rating">
                <StarRating rating={book.averageRating} />
                <span className="rating-text">{book.averageRating.toFixed(2)}</span>
              </div>
            )}
            {book.pubYear > 0 && <span className="book-card-year">{book.pubYear}</span>}
            {book.ratingsCount > 0 && (
              <span className="book-card-count">{book.ratingsCount.toLocaleString()} ratings</span>
            )}
          </div>
          {genreInfo && (
            <span
              className="book-card-genre-badge"
              style={{ background: genreInfo.bg, color: genreInfo.text }}
            >
              {genreInfo.label}
            </span>
          )}
        </div>
      </Link>
    </div>
  );
}

export default BookCard;
