import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
});

// Books
export const getBooks = (page = 0, size = 20, sortBy = 'ratingsCount', direction = 'DESC', genre = null) => {
  const params = { page, size, sortBy, direction };
  if (genre) params.genre = genre;
  return api.get('/books', { params }).then(r => r.data);
};

export const getBookById = (bookId) =>
  api.get(`/books/${bookId}`).then(r => r.data);

export const getBookReviews = (bookId, page = 0, size = 10) =>
  api.get(`/books/${bookId}/reviews`, { params: { page, size } }).then(r => r.data);

export const getSimilarBooks = (bookId, limit = 10) =>
  api.get(`/books/${bookId}/similar`, { params: { limit } }).then(r => r.data);

// Search
export const search = (q, page = 0, size = 20, filters = {}) =>
  api.get('/search', { params: { q, page, size, ...filters } }).then(r => r.data);

export const autocomplete = (q, limit = 5) =>
  api.get('/search/autocomplete', { params: { q, limit } }).then(r => r.data);

// Authors
export const getAuthor = (authorId) =>
  api.get(`/authors/${authorId}`).then(r => r.data);

export const getAuthorBooks = (authorId, page = 0, size = 20) =>
  api.get(`/authors/${authorId}/books`, { params: { page, size } }).then(r => r.data);

// Genres
export const getGenres = () =>
  api.get('/genres').then(r => r.data);

export const getGenreBooks = (genreKey, page = 0, size = 20, sortBy = 'ratingsCount', direction = 'DESC') =>
  api.get(`/genres/${genreKey}/books`, { params: { page, size, sortBy, direction } }).then(r => r.data);

export const getGenreTopShelves = (genreKey, limit = 20) =>
  api.get(`/genres/${genreKey}/top-shelves`, { params: { limit } }).then(r => r.data);

export const getAllTopShelves = (limit = 50) =>
  api.get('/genres/all/top-shelves', { params: { limit } }).then(r => r.data);

// Moods
export const getMoods = () =>
  api.get('/moods').then(r => r.data);

export const getMood = (moodKey) =>
  api.get(`/moods/${moodKey}`).then(r => r.data);

export const getMoodBooks = (moodKey, limit = 20, genre = 'all') =>
  api.get(`/moods/${moodKey}/books`, { params: { limit, genre } }).then(r => r.data);

export const getCustomMoodBooks = (shelves, genre = null, limit = 20) =>
  api.post('/moods/custom/books', { shelves, genre, limit }).then(r => r.data);

// Recommendations
export const getRecommendations = (bookId, strategy = 'hybrid', limit = 10) =>
  api.get(`/recommendations/similar/${bookId}`, { params: { strategy, limit } }).then(r => r.data);

export const getReadersAlsoLiked = (bookId, limit = 10) =>
  api.get(`/recommendations/readers-also-liked/${bookId}`, { params: { limit } }).then(r => r.data);

export const getShelfRecommendations = (shelfName, limit = 20) =>
  api.get(`/recommendations/shelf/${shelfName}`, { params: { limit } }).then(r => r.data);

export const getAuthorRecommendations = (authorId, limit = 10) =>
  api.get(`/recommendations/author/${authorId}`, { params: { limit } }).then(r => r.data);

// Graph
export const getBookGraph = (bookId, depth = 1, includeUsers = false) =>
  api.get(`/graph/book/${bookId}`, { params: { depth, includeUsers } }).then(r => r.data);

export const getAuthorGraph = (authorId) =>
  api.get(`/graph/author/${authorId}`).then(r => r.data);

export const getShelfGraph = (shelfName, limit = 20) =>
  api.get(`/graph/shelf/${shelfName}`, { params: { limit } }).then(r => r.data);

export const getRecommendationGraph = (bookId) =>
  api.get(`/graph/recommendations/${bookId}`).then(r => r.data);

export default api;
