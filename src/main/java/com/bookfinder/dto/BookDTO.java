package com.bookfinder.dto;

import java.util.List;

public class BookDTO {
    private String bookId;
    private String title;
    private String titleClean;
    private String description;
    private Double averageRating;
    private Integer ratingsCount;
    private Integer numPages;
    private String publisher;
    private Integer pubYear;
    private String imageUrl;
    private String url;
    private String workId;
    private List<AuthorDTO> authors;
    private List<ShelfDTO> shelves;
    private String genre;
    private List<String> seriesIds;

    public BookDTO() {}

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTitleClean() { return titleClean; }
    public void setTitleClean(String titleClean) { this.titleClean = titleClean; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
    public Integer getRatingsCount() { return ratingsCount; }
    public void setRatingsCount(Integer ratingsCount) { this.ratingsCount = ratingsCount; }
    public Integer getNumPages() { return numPages; }
    public void setNumPages(Integer numPages) { this.numPages = numPages; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public Integer getPubYear() { return pubYear; }
    public void setPubYear(Integer pubYear) { this.pubYear = pubYear; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getWorkId() { return workId; }
    public void setWorkId(String workId) { this.workId = workId; }
    public List<AuthorDTO> getAuthors() { return authors; }
    public void setAuthors(List<AuthorDTO> authors) { this.authors = authors; }
    public List<ShelfDTO> getShelves() { return shelves; }
    public void setShelves(List<ShelfDTO> shelves) { this.shelves = shelves; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public List<String> getSeriesIds() { return seriesIds; }
    public void setSeriesIds(List<String> seriesIds) { this.seriesIds = seriesIds; }
}
