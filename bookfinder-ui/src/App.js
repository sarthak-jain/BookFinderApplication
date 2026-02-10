import React from 'react';
import { Routes, Route } from 'react-router-dom';
import Header from './components/Header';
import Footer from './components/Footer';
import HomePage from './pages/HomePage';
import SearchPage from './pages/SearchPage';
import BookDetailPage from './pages/BookDetailPage';
import ExplorePage from './pages/ExplorePage';
import RecommendationsPage from './pages/RecommendationsPage';
import './App.css';

function App() {
  return (
    <div className="app">
      <Header />
      <main className="main-content">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/search" element={<SearchPage />} />
          <Route path="/books/:bookId" element={<BookDetailPage />} />
          <Route path="/explore" element={<ExplorePage />} />
          <Route path="/recommendations/:bookId" element={<RecommendationsPage />} />
        </Routes>
      </main>
      <Footer />
    </div>
  );
}

export default App;
