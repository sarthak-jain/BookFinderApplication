import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getBookById, getRecommendationGraph } from '../services/api';
import GraphVisualization from '../components/GraphVisualization';
import RecommendationPanel from '../components/RecommendationPanel';
import './RecommendationsPage.css';

function RecommendationsPage() {
  const { bookId } = useParams();
  const [book, setBook] = useState(null);
  const [graphData, setGraphData] = useState(null);
  const [loading, setLoading] = useState(true);


  useEffect(() => {
    setLoading(true);
    Promise.all([
      getBookById(bookId),
      getRecommendationGraph(bookId),
    ])
      .then(([bookData, graph]) => {
        setBook(bookData);
        setGraphData(graph);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  }, [bookId]);

  if (loading) return <div className="loading-spinner">Loading recommendations...</div>;

  return (
    <div className="container recommendations-page">
      <div className="rec-page-header">
        <h1>Recommendations</h1>
        {book && (
          <p>
            Based on <Link to={`/books/${bookId}`}>{book.title}</Link>
          </p>
        )}
      </div>

      <div className="rec-graph-section">
        <div className="rec-graph-header">
          <h2>Recommendation Graph</h2>
          <div className="rec-graph-legend-text">
            <span style={{color: '#4A90D9'}}>Blue edges</span> = Similar To |
            <span style={{color: '#5CB85C'}}> Green edges</span> = Shelf Similar |
            <span style={{color: '#E8913A'}}> Orange edges</span> = Readers Also Liked
          </div>
        </div>
        {graphData && (
          <GraphVisualization graphData={graphData} height="500px" />
        )}
      </div>

      <div className="rec-list-section">
        <h2>Recommended Books</h2>
        <RecommendationPanel bookId={bookId} />
      </div>
    </div>
  );
}

export default RecommendationsPage;
