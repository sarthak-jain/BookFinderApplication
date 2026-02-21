package com.bookfinder.service;

import com.bookfinder.dto.BookSearchResultDTO;
import com.bookfinder.dto.MoodDTO;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.types.Node;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MoodService {

    private static final Map<String, MoodDTO> MOODS = new LinkedHashMap<>();

    static {
        MOODS.put("adventurous", new MoodDTO("adventurous", "Feeling Adventurous",
                "Epic quests and thrilling journeys", "#E67E22",
                List.of("adventure", "fantasy", "action", "quest", "epic")));
        MOODS.put("romantic", new MoodDTO("romantic", "In the Mood for Love",
                "Heart-fluttering stories", "#E91E63",
                List.of("romance", "love", "contemporary-romance", "love-story")));
        MOODS.put("suspenseful", new MoodDTO("suspenseful", "On the Edge",
                "Can't-put-it-down suspense", "#9C27B0",
                List.of("thriller", "mystery", "suspense", "crime", "detective")));
        MOODS.put("feel-good", new MoodDTO("feel-good", "Feel-Good Vibes",
                "Uplifting and heartwarming reads", "#4CAF50",
                List.of("humor", "funny", "feel-good", "heartwarming", "comedy")));
        MOODS.put("dark", new MoodDTO("dark", "Dark & Gritty",
                "Intense and haunting stories", "#37474F",
                List.of("dark", "horror", "gothic", "dystopia", "post-apocalyptic")));
        MOODS.put("mind-bending", new MoodDTO("mind-bending", "Mind-Bending",
                "Stories that twist reality", "#2196F3",
                List.of("science-fiction", "time-travel", "dystopia", "paranormal")));
        MOODS.put("emotional", new MoodDTO("emotional", "Need a Good Cry",
                "Deeply moving stories", "#FF7043",
                List.of("emotional", "sad", "heartbreaking", "drama", "tear-jerker")));
        MOODS.put("intellectual", new MoodDTO("intellectual", "Intellectually Curious",
                "Learn something new", "#795548",
                List.of("history", "biography", "non-fiction", "science", "philosophy")));
        MOODS.put("quick-escape", new MoodDTO("quick-escape", "Quick Escape",
                "Light, fast reads to unwind", "#00BCD4",
                List.of("short-stories", "novella", "contemporary", "light-read")));
        MOODS.put("epic-journey", new MoodDTO("epic-journey", "Epic Journey",
                "Grand sagas and sprawling worlds", "#FF9800",
                List.of("epic", "saga", "series", "world-building", "high-fantasy")));
    }

    private final Driver driver;

    public MoodService(Driver driver) {
        this.driver = driver;
    }

    public List<MoodDTO> getAllMoods() {
        return new ArrayList<>(MOODS.values());
    }

    public MoodDTO getMood(String moodKey) {
        return MOODS.get(moodKey);
    }

    public List<BookSearchResultDTO> getMoodBooks(String moodKey, int limit, String genre) {
        MoodDTO mood = MOODS.get(moodKey);
        if (mood == null) return List.of();
        return findBooksByShelves(mood.getShelves(), limit, genre);
    }

    public List<BookSearchResultDTO> getCustomMoodBooks(List<String> shelves, int limit, String genre) {
        if (shelves == null || shelves.isEmpty()) return List.of();
        return findBooksByShelves(shelves, limit, genre);
    }

    private List<BookSearchResultDTO> findBooksByShelves(List<String> shelves, int limit, String genre) {
        Map<String, Object> params = new HashMap<>();
        params.put("shelves", shelves);
        params.put("limit", limit);

        String genreFilter = "";
        if (genre != null && !genre.isBlank() && !"all".equalsIgnoreCase(genre)) {
            genreFilter = "AND b.genre = $genre\n";
            params.put("genre", genre);
        }

        String query = String.format("""
            MATCH (b:Book)-[r:SHELVED_AS]->(s:Shelf)
            WHERE s.name IN $shelves
            %s
            WITH b, count(DISTINCT s) AS shelfMatches, sum(r.count) AS totalShelfCount
            RETURN b, shelfMatches, totalShelfCount
            ORDER BY shelfMatches DESC, totalShelfCount DESC, b.ratingsCount DESC
            LIMIT $limit
            """, genreFilter);

        try (Session session = session()) {
            var result = session.run(query, params);

            List<BookSearchResultDTO> books = new ArrayList<>();
            while (result.hasNext()) {
                Record rec = result.next();
                Node node = rec.get("b").asNode();
                BookSearchResultDTO dto = toSearchResult(node);
                dto.setScore(rec.get("shelfMatches").asDouble(0));
                books.add(dto);
            }
            return books;
        }
    }

    private Session session() {
        return driver.session(SessionConfig.forDatabase("neo4j"));
    }

    private BookSearchResultDTO toSearchResult(Node node) {
        BookSearchResultDTO dto = new BookSearchResultDTO();
        dto.setBookId(node.get("bookId").asString(""));
        dto.setTitle(node.get("title").asString(""));
        dto.setTitleClean(node.get("titleClean").asString(""));
        dto.setAverageRating(node.get("averageRating").asDouble(0));
        dto.setRatingsCount(node.get("ratingsCount").asInt(0));
        dto.setImageUrl(node.get("imageUrl").asString(""));
        dto.setPublisher(node.get("publisher").asString(""));
        dto.setPubYear(node.get("pubYear").asInt(0));
        dto.setGenre(node.get("genre").asString(""));
        return dto;
    }
}
