package com.bookfinder.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

@Node("Book")
public class Book {

    @Id
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
    private String isbn;
    private String isbn13;
    private String asin;

    @Relationship(type = "WROTE", direction = Relationship.Direction.INCOMING)
    private Set<Author> authors = new HashSet<>();

    @Relationship(type = "IN_SERIES", direction = Relationship.Direction.OUTGOING)
    private Set<Series> series = new HashSet<>();

    @Relationship(type = "SHELVED_AS", direction = Relationship.Direction.OUTGOING)
    private Set<Shelf> shelves = new HashSet<>();

    @Relationship(type = "SIMILAR_TO", direction = Relationship.Direction.OUTGOING)
    private Set<Book> similarBooks = new HashSet<>();

    public Book() {}

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
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getIsbn13() { return isbn13; }
    public void setIsbn13(String isbn13) { this.isbn13 = isbn13; }
    public String getAsin() { return asin; }
    public void setAsin(String asin) { this.asin = asin; }
    public Set<Author> getAuthors() { return authors; }
    public void setAuthors(Set<Author> authors) { this.authors = authors; }
    public Set<Series> getSeries() { return series; }
    public void setSeries(Set<Series> series) { this.series = series; }
    public Set<Shelf> getShelves() { return shelves; }
    public void setShelves(Set<Shelf> shelves) { this.shelves = shelves; }
    public Set<Book> getSimilarBooks() { return similarBooks; }
    public void setSimilarBooks(Set<Book> similarBooks) { this.similarBooks = similarBooks; }
}
