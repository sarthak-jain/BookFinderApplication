package com.bookfinder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "bookfinder.data")
public class DataLoadProperties {

    private String dir = "./data";
    private String booksFile = "goodreads_books_young_adult.json";
    private String interactionsFile = "goodreads_interactions_young_adult.json";
    private String reviewsFile = "goodreads_reviews_young_adult.json";
    private int subsetSize = 10000;
    private int maxInteractions = 50000;
    private int maxReviews = 50000;
    private int batchSize = 500;

    public String getDir() { return dir; }
    public void setDir(String dir) { this.dir = dir; }
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
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

    public String getBooksPath() { return dir + "/" + booksFile; }
    public String getInteractionsPath() { return dir + "/" + interactionsFile; }
    public String getReviewsPath() { return dir + "/" + reviewsFile; }
}
