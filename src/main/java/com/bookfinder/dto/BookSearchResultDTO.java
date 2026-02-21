package com.bookfinder.dto;

public class BookSearchResultDTO {
    private String bookId;
    private String title;
    private String titleClean;
    private Double averageRating;
    private Integer ratingsCount;
    private String imageUrl;
    private String publisher;
    private Integer pubYear;
    private String genre;
    private Double score;

    public BookSearchResultDTO() {}

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTitleClean() { return titleClean; }
    public void setTitleClean(String titleClean) { this.titleClean = titleClean; }
    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
    public Integer getRatingsCount() { return ratingsCount; }
    public void setRatingsCount(Integer ratingsCount) { this.ratingsCount = ratingsCount; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public Integer getPubYear() { return pubYear; }
    public void setPubYear(Integer pubYear) { this.pubYear = pubYear; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}
