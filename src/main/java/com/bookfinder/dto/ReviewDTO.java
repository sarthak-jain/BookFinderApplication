package com.bookfinder.dto;

public class ReviewDTO {
    private String reviewId;
    private String userId;
    private String bookId;
    private Integer rating;
    private String reviewText;
    private Integer nVotes;
    private Integer nComments;
    private String dateAdded;

    public ReviewDTO() {}

    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }
    public Integer getNVotes() { return nVotes; }
    public void setNVotes(Integer nVotes) { this.nVotes = nVotes; }
    public Integer getNComments() { return nComments; }
    public void setNComments(Integer nComments) { this.nComments = nComments; }
    public String getDateAdded() { return dateAdded; }
    public void setDateAdded(String dateAdded) { this.dateAdded = dateAdded; }
}
