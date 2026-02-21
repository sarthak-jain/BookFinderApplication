import React from 'react';
import { Routes, Route } from 'react-router-dom';
import Header from './components/Header';
import Footer from './components/Footer';
import HomePage from './pages/HomePage';
import SearchPage from './pages/SearchPage';
import BookDetailPage from './pages/BookDetailPage';
import ExplorePage from './pages/ExplorePage';
import RecommendationsPage from './pages/RecommendationsPage';
import GenrePage from './pages/GenrePage';
import MoodsPage from './pages/MoodsPage';
import MoodPage from './pages/MoodPage';
import CustomMoodPage from './pages/CustomMoodPage';
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
          <Route path="/genres/:genreKey" element={<GenrePage />} />
          <Route path="/moods" element={<MoodsPage />} />
          <Route path="/moods/custom" element={<CustomMoodPage />} />
          <Route path="/moods/:moodKey" element={<MoodPage />} />
        </Routes>
      </main>
      <Footer />
    </div>
  );
}

export default App;
