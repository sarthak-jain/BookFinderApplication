package com.bookfinder.controller;

import com.bookfinder.dto.GraphVisualizationDTO;
import com.bookfinder.service.GraphService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/book/{bookId}")
    public GraphVisualizationDTO bookNeighborhood(
            @PathVariable String bookId,
            @RequestParam(defaultValue = "1") int depth,
            @RequestParam(defaultValue = "false") boolean includeUsers) {
        return graphService.bookNeighborhood(bookId, Math.min(depth, 3), includeUsers);
    }

    @GetMapping("/author/{authorId}")
    public GraphVisualizationDTO authorGraph(@PathVariable String authorId) {
        return graphService.authorGraph(authorId);
    }

    @GetMapping("/shelf/{shelfName}")
    public GraphVisualizationDTO shelfGraph(
            @PathVariable String shelfName,
            @RequestParam(defaultValue = "20") int limit) {
        return graphService.shelfGraph(shelfName, limit);
    }

    @GetMapping("/recommendations/{bookId}")
    public GraphVisualizationDTO recommendationGraph(@PathVariable String bookId) {
        return graphService.recommendationGraph(bookId);
    }
}
