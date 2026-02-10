import React from 'react';
import BookCard from './BookCard';
import './BookList.css';

function BookList({ books }) {
  if (!books || books.length === 0) {
    return (
      <div className="empty-state">
        <h3>No books found</h3>
        <p>Try adjusting your search or filters.</p>
      </div>
    );
  }

  return (
    <div className="book-list">
      {books.map((book) => (
        <BookCard key={book.bookId} book={book} />
      ))}
    </div>
  );
}

export default BookList;
