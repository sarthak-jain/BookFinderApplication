package com.bookfinder.loader;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class SubsetSelector {

    private static final Logger log = LoggerFactory.getLogger(SubsetSelector.class);

    public static Set<String> selectTopBookIds(String booksFilePath, int subsetSize) throws IOException {
        log.info("Scanning books file to select top {} by ratings_count...", subsetSize);

        PriorityQueue<BookEntry> minHeap = new PriorityQueue<>(
                Comparator.comparingInt(e -> e.ratingsCount)
        );

        int total = 0;
        try (JsonLineReader reader = new JsonLineReader(booksFilePath)) {
            Iterator<JsonNode> it = reader.stream().iterator();
            while (it.hasNext()) {
                JsonNode node = it.next();
                String bookId = node.path("book_id").asText("");
                int ratingsCount = parseIntSafe(node.path("ratings_count").asText("0"));

                if (!bookId.isEmpty() && ratingsCount > 0) {
                    minHeap.offer(new BookEntry(bookId, ratingsCount));
                    if (minHeap.size() > subsetSize) {
                        minHeap.poll();
                    }
                }
                total++;
                if (total % 10000 == 0) {
                    log.info("  Scanned {} books...", total);
                }
            }
        }

        Set<String> selectedIds = new HashSet<>();
        for (BookEntry entry : minHeap) {
            selectedIds.add(entry.bookId);
        }

        log.info("Selected {} books from {} total (min ratings_count in subset: {})",
                selectedIds.size(), total,
                minHeap.isEmpty() ? 0 : minHeap.peek().ratingsCount);

        return selectedIds;
    }

    private static int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private record BookEntry(String bookId, int ratingsCount) {}
}
