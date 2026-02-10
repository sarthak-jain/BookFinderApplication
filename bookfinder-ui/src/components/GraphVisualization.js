import React, { useRef, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Network } from 'vis-network';
import { DataSet } from 'vis-data';
import './GraphVisualization.css';

const LEGEND = [
  { label: 'Book', color: '#4A90D9' },
  { label: 'Author', color: '#E8913A' },
  { label: 'Shelf', color: '#5CB85C' },
  { label: 'Series', color: '#9B59B6' },
  { label: 'User', color: '#95A5A6' },
];

function GraphVisualization({ graphData, height = '500px' }) {
  const containerRef = useRef(null);
  const networkRef = useRef(null);
  const navigate = useNavigate();
  const [selectedNode, setSelectedNode] = useState(null);

  useEffect(() => {
    if (!containerRef.current || !graphData || !graphData.nodes) return;

    const nodes = new DataSet(
      graphData.nodes.map((n) => ({
        id: n.id,
        label: n.label,
        color: {
          background: n.color,
          border: n.color,
          highlight: { background: n.color, border: '#333' },
        },
        size: n.size || 20,
        shape: n.type === 'Book' ? 'dot' : n.type === 'Author' ? 'diamond' : n.type === 'Shelf' ? 'triangle' : 'dot',
        font: { color: '#333', size: 12, face: 'Inter, sans-serif' },
        title: n.label,
        nodeData: n,
      }))
    );

    const edges = new DataSet(
      graphData.edges.map((e, i) => ({
        id: `edge_${i}`,
        from: e.from,
        to: e.to,
        label: e.label,
        color: { color: e.color || '#CCCCCC', highlight: '#4A90D9' },
        font: { size: 10, color: '#999', strokeWidth: 0 },
        arrows: 'to',
        smooth: { type: 'continuous' },
      }))
    );

    const options = {
      physics: {
        solver: 'forceAtlas2Based',
        forceAtlas2Based: {
          gravitationalConstant: -40,
          centralGravity: 0.01,
          springLength: 120,
          springConstant: 0.05,
          damping: 0.4,
        },
        stabilization: { iterations: 150 },
      },
      interaction: {
        hover: true,
        tooltipDelay: 200,
        navigationButtons: true,
        keyboard: true,
      },
      nodes: {
        borderWidth: 2,
        shadow: true,
      },
      edges: {
        width: 1.5,
      },
    };

    const network = new Network(containerRef.current, { nodes, edges }, options);
    networkRef.current = network;

    network.on('click', (params) => {
      if (params.nodes.length > 0) {
        const nodeId = params.nodes[0];
        const node = nodes.get(nodeId);
        setSelectedNode(node?.nodeData || null);

        if (node?.nodeData?.type === 'Book' && node?.nodeData?.properties?.bookId) {
          navigate(`/books/${node.nodeData.properties.bookId}`);
        }
      } else {
        setSelectedNode(null);
      }
    });

    return () => {
      network.destroy();
    };
  }, [graphData, navigate]);

  if (!graphData || !graphData.nodes || graphData.nodes.length === 0) {
    return <div className="empty-state"><h3>No graph data available</h3></div>;
  }

  return (
    <div className="graph-visualization">
      <div className="graph-legend">
        {LEGEND.map((item) => (
          <span key={item.label} className="legend-item">
            <span className="legend-dot" style={{ backgroundColor: item.color }} />
            {item.label}
          </span>
        ))}
      </div>
      <div ref={containerRef} className="graph-container" style={{ height }} />
      {selectedNode && (
        <div className="graph-node-info">
          <strong>{selectedNode.label}</strong>
          <span className="node-type-badge">{selectedNode.type}</span>
        </div>
      )}
    </div>
  );
}

export default GraphVisualization;
