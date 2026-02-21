package com.bookfinder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "bookfinder.data")
public class DataLoadProperties {

    private String dir = "./data";
    private String authorsFile = "goodreads_book_authors.json/goodreads_book_authors.json";
    private int batchSize = 500;
    private List<GenreConfig> genres = new ArrayList<>();

    public String getDir() { return dir; }
    public void setDir(String dir) { this.dir = dir; }
    public String getAuthorsFile() { return authorsFile; }
    public void setAuthorsFile(String authorsFile) { this.authorsFile = authorsFile; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public List<GenreConfig> getGenres() { return genres; }
    public void setGenres(List<GenreConfig> genres) { this.genres = genres; }

    public String getAuthorsPath() { return dir + "/" + authorsFile; }

    public static class GenreConfig {
        private String name;
        private String key;
        private String folder;
        private String booksFile;
        private String interactionsFile;
        private String reviewsFile;
        private int subsetSize = 15000;
        private int maxInteractions = 30000;
        private int maxReviews = 30000;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getFolder() { return folder; }
        public void setFolder(String folder) { this.folder = folder; }
        public String getBooksFile() { return booksFile; }
        public void setBooksFile(String booksFile) { this.booksFile = booksFile; }
        public String getInteractionsFile() { return interactionsFile; }
        public void setInteractionsFile(String interactionsFile) { this.interactionsFile = interactionsFile; }
        public String getReviewsFile() { return reviewsFile; }
        public void setReviewsFile(String reviewsFile) { this.reviewsFile = reviewsFile; }
        public int getSubsetSize() { return subsetSize; }
        public void setSubsetSize(int subsetSize) { this.subsetSize = subsetSize; }
        public int getMaxInteractions() { return maxInteractions; }
        public void setMaxInteractions(int maxInteractions) { this.maxInteractions = maxInteractions; }
        public int getMaxReviews() { return maxReviews; }
        public void setMaxReviews(int maxReviews) { this.maxReviews = maxReviews; }

        public String getBasePath(String dataDir) {
            return dataDir + "/" + folder;
        }

        public String getBooksPath(String dataDir) {
            return getBasePath(dataDir) + "/" + booksFile;
        }

        public String getInteractionsPath(String dataDir) {
            return getBasePath(dataDir) + "/" + interactionsFile;
        }

        public String getReviewsPath(String dataDir) {
            return getBasePath(dataDir) + "/" + reviewsFile;
        }
    }
}
