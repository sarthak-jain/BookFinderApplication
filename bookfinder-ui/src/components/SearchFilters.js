import React, { useState } from 'react';
import './SearchFilters.css';

function SearchFilters({ onFilterChange, filters = {} }) {
  const [minRating, setMinRating] = useState(filters.minRating || '');
  const [minYear, setMinYear] = useState(filters.minYear || '');
  const [maxYear, setMaxYear] = useState(filters.maxYear || '');

  const handleApply = () => {
    onFilterChange({
      minRating: minRating ? Number(minRating) : undefined,
      minYear: minYear ? Number(minYear) : undefined,
      maxYear: maxYear ? Number(maxYear) : undefined,
    });
  };

  const handleClear = () => {
    setMinRating('');
    setMinYear('');
    setMaxYear('');
    onFilterChange({});
  };

  return (
    <div className="search-filters">
      <h4 className="filters-title">Filters</h4>
      <div className="filter-row">
        <label>Min Rating</label>
        <input
          type="number"
          min="0" max="5" step="0.5"
          value={minRating}
          onChange={(e) => setMinRating(e.target.value)}
          placeholder="0-5"
        />
      </div>
      <div className="filter-row">
        <label>Year Range</label>
        <div className="filter-range">
          <input
            type="number"
            value={minYear}
            onChange={(e) => setMinYear(e.target.value)}
            placeholder="From"
          />
          <span>-</span>
          <input
            type="number"
            value={maxYear}
            onChange={(e) => setMaxYear(e.target.value)}
            placeholder="To"
          />
        </div>
      </div>
      <div className="filter-actions">
        <button className="filter-apply" onClick={handleApply}>Apply</button>
        <button className="filter-clear" onClick={handleClear}>Clear</button>
      </div>
    </div>
  );
}

export default SearchFilters;
