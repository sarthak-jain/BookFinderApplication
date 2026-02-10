import React from 'react';
import './StarRating.css';

function StarRating({ rating, maxStars = 5 }) {
  const stars = [];
  const fullStars = Math.floor(rating);
  const partialFill = (rating - fullStars) * 100;

  for (let i = 0; i < maxStars; i++) {
    if (i < fullStars) {
      stars.push(<span key={i} className="star star-full">&#9733;</span>);
    } else if (i === fullStars && partialFill > 0) {
      stars.push(
        <span key={i} className="star star-partial" style={{ '--fill': `${partialFill}%` }}>
          &#9733;
        </span>
      );
    } else {
      stars.push(<span key={i} className="star star-empty">&#9733;</span>);
    }
  }

  return <span className="star-rating">{stars}</span>;
}

export default StarRating;
