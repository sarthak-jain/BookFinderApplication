import React from 'react';
import './Footer.css';

function Footer() {
  return (
    <footer className="footer">
      <div className="container">
        <p>BookFinder - Book Recommendation Engine | Powered by Neo4j Graph Database</p>
        <p className="footer-sub">Data sourced from Goodreads dataset across multiple genres</p>
        <p className="footer-love">Made with ❤️  by Sarthak Jain</p>
      </div>
    </footer>
  );
}

export default Footer;
