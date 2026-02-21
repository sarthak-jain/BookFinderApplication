import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getMoods } from '../services/api';
import './MoodsPage.css';

function MoodsPage() {
  const [moods, setMoods] = useState([]);

  useEffect(() => {
    getMoods().then(setMoods).catch(() => {});
  }, []);

  return (
    <div className="container moods-page">
      <h1>Reading Moods</h1>
      <p className="moods-subtitle">Choose a mood to find the perfect book, or build your own.</p>

      <div className="moods-grid">
        {moods.map((mood) => (
          <Link
            key={mood.key}
            to={`/moods/${mood.key}`}
            className="mood-tile"
          >
            <div className="mood-tile-bar" style={{ background: mood.color }} />
            <h3>{mood.name}</h3>
            <p>{mood.description}</p>
            <div className="mood-tile-shelves">
              {mood.shelves?.slice(0, 3).map((s) => (
                <span key={s} className="mood-shelf-tag">{s}</span>
              ))}
            </div>
          </Link>
        ))}

        <Link to="/moods/custom" className="mood-tile mood-tile-custom">
          <div className="mood-tile-bar" style={{ background: '#4A90D9' }} />
          <h3>Build Your Mood</h3>
          <p>Pick shelves to create a custom mood blend</p>
        </Link>
      </div>
    </div>
  );
}

export default MoodsPage;
