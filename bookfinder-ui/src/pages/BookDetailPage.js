import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getBookById, getBookReviews, getSimilarBooks } from '../services/api';
import StarRating from '../components/StarRating';
import BookList from '../components/BookList';
import RecommendationPanel from '../components/RecommendationPanel';
import Pagination from '../components/Pagination';
import './BookDetailPage.css';

function BookDetailPage() {
  const { bookId } = useParams();
  const [book, setBook] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [reviewPage, setReviewPage] = useState(0);
  const [reviewTotalPages, setReviewTotalPages] = useState(0);
  const [similarBooks, setSimilarBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('details');

  useEffect(() => {
    setLoading(true);
    setError(null);
    setActiveTab('details');
    setReviewPage(0);

    Promise.all([
      getBookById(bookId),
      getSimilarBooks(bookId, 6),
    ])
      .then(([bookData, similar]) => {
        setBook(bookData);
        setSimilarBooks(similar);
        setLoading(false);
      })
      .catch(() => {
        setError('Failed to load book details.');
        setLoading(false);
      });
  }, [bookId]);

  useEffect(() => {
    if (activeTab === 'reviews') {
      getBookReviews(bookId, reviewPage, 10)
        .then((data) => {
          setReviews(data.content);
          setReviewTotalPages(data.totalPages);
        })
        .catch(() => {});
    }
  }, [bookId, reviewPage, activeTab]);

  if (loading) return <div className="loading-spinner">Loading book details...</div>;
  if (error) return <div className="container"><div className="error-banner">{error}</div></div>;
  if (!book) return <div className="container"><div className="empty-state"><h3>Book not found</h3></div></div>;

  const hasImage = book.imageUrl && !book.imageUrl.includes('nophoto');

  return (
    <div className="container book-detail">
      <div className="book-detail-header">
        <div className="book-detail-cover">
          {hasImage ? (
            <img src={book.imageUrl} alt={book.title} />
          ) : (
            <div className="book-detail-placeholder">
              <span>{(book.titleClean || book.title || '?').charAt(0)}</span>
            </div>
          )}
        </div>
        <div className="book-detail-info">
          <h1>{book.title}</h1>
          {book.authors && book.authors.length > 0 && (
            <div className="book-authors">
              by {book.authors.map((a, i) => (
                <span key={a.authorId}>
                  <Link to={`/explore?authorId=${a.authorId}`}>{a.authorId}</Link>
                  {a.role && <span className="author-role"> ({a.role})</span>}
                  {i < book.authors.length - 1 && ', '}
                </span>
              ))}
            </div>
          )}
          <div className="book-rating-info">
            {book.averageRating > 0 && (
              <>
                <StarRating rating={book.averageRating} />
                <span className="rating-number">{book.averageRating.toFixed(2)}</span>
              </>
            )}
            {book.ratingsCount > 0 && (
              <span className="ratings-count">{book.ratingsCount.toLocaleString()} ratings</span>
            )}
          </div>
          <div className="book-meta">
            {book.pubYear > 0 && <span>Published: {book.pubYear}</span>}
            {book.numPages > 0 && <span>{book.numPages} pages</span>}
            {book.publisher && <span>Publisher: {book.publisher}</span>}
          </div>
          {book.shelves && book.shelves.length > 0 && (
            <div className="book-shelves">
              {book.shelves.slice(0, 15).map((shelf) => (
                <Link key={shelf.name} to={`/explore?shelf=${shelf.name}`} className="shelf-tag">
                  {shelf.name}
                </Link>
              ))}
            </div>
          )}
          <div className="book-actions">
            <Link to={`/recommendations/${bookId}`} className="btn-recommend">
              View Recommendations
            </Link>
            {book.url && (
              <a href={book.url} target="_blank" rel="noopener noreferrer" className="btn-external">
                View on Goodreads
              </a>
            )}
          </div>
        </div>
      </div>

      <div className="book-tabs">
        <button className={activeTab === 'details' ? 'tab active' : 'tab'} onClick={() => setActiveTab('details')}>Description</button>
        <button className={activeTab === 'reviews' ? 'tab active' : 'tab'} onClick={() => setActiveTab('reviews')}>Reviews</button>
        <button className={activeTab === 'similar' ? 'tab active' : 'tab'} onClick={() => setActiveTab('similar')}>Similar</button>
        <button className={activeTab === 'recommendations' ? 'tab active' : 'tab'} onClick={() => setActiveTab('recommendations')}>Recommendations</button>
      </div>

      <div className="book-tab-content">
        {activeTab === 'details' && (
          <div className="book-description">
            {book.description ? (
              <p>{book.description}</p>
            ) : (
              <p className="no-description">No description available.</p>
            )}
          </div>
        )}

        {activeTab === 'reviews' && (
          <div className="book-reviews">
            {reviews.length === 0 ? (
              <div className="empty-state"><h3>No reviews yet</h3></div>
            ) : (
              <>
                {reviews.map((review) => (
                  <div key={review.reviewId} className="review-card">
                    <div className="review-header">
                      <StarRating rating={review.rating || 0} />
                      <span className="review-date">{review.dateAdded}</span>
                    </div>
                    <p className="review-text">{review.reviewText}</p>
                    <div className="review-footer">
                      <span>{review.nVotes} helpful votes</span>
                      <span>{review.nComments} comments</span>
                    </div>
                  </div>
                ))}
                <Pagination page={reviewPage} totalPages={reviewTotalPages} onPageChange={setReviewPage} />
              </>
            )}
          </div>
        )}

        {activeTab === 'similar' && (
          <div>
            <BookList books={similarBooks} />
          </div>
        )}

        {activeTab === 'recommendations' && (
          <RecommendationPanel bookId={bookId} />
        )}
      </div>
    </div>
  );
}

export default BookDetailPage;
