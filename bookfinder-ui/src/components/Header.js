import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import SearchBar from './SearchBar';
import './Header.css';

function Header() {
  const navigate = useNavigate();

  const handleSearch = (query) => {
    navigate(`/search?q=${encodeURIComponent(query)}`);
  };

  return (
    <header className="header">
      <div className="header-inner container">
        <Link to="/" className="logo">
          <span className="logo-icon">B</span>
          <span className="logo-text">BookFinder</span>
        </Link>
        <div className="header-search">
          <SearchBar onSearch={handleSearch} />
        </div>
        <nav className="header-nav">
          <Link to="/">Home</Link>
          <Link to="/explore">Explore</Link>
        </nav>
      </div>
    </header>
  );
}

export default Header;
