import React from 'react';
import { Link } from 'react-router-dom';
import StarRating from './StarRating';
import './BookCard.css';

function BookCard({ book }) {
  const hasImage = book.imageUrl && !book.imageUrl.includes('nophoto');

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
        </div>
      </Link>
    </div>
  );
}

export default BookCard;
