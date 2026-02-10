package com.bookfinder.dto;

import java.util.List;

public class GraphVisualizationDTO {
    private List<NodeDTO> nodes;
    private List<EdgeDTO> edges;

    public GraphVisualizationDTO() {}

    public GraphVisualizationDTO(List<NodeDTO> nodes, List<EdgeDTO> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public List<NodeDTO> getNodes() { return nodes; }
    public void setNodes(List<NodeDTO> nodes) { this.nodes = nodes; }
    public List<EdgeDTO> getEdges() { return edges; }
    public void setEdges(List<EdgeDTO> edges) { this.edges = edges; }

    public static class NodeDTO {
        private String id;
        private String label;
        private String type;
        private String color;
        private Integer size;
        private Object properties;

        public NodeDTO() {}

        public NodeDTO(String id, String label, String type, String color, Integer size, Object properties) {
            this.id = id;
            this.label = label;
            this.type = type;
            this.color = color;
            this.size = size;
            this.properties = properties;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
        public Object getProperties() { return properties; }
        public void setProperties(Object properties) { this.properties = properties; }
    }

    public static class EdgeDTO {
        private String from;
        private String to;
        private String label;
        private String color;

        public EdgeDTO() {}

        public EdgeDTO(String from, String to, String label, String color) {
            this.from = from;
            this.to = to;
            this.label = label;
            this.color = color;
        }

        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }
}
