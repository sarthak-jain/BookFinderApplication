package com.bookfinder.dto;

import java.util.List;

public class CustomMoodRequest {
    private List<String> shelves;
    private String genre;
    private int limit = 20;

    public List<String> getShelves() { return shelves; }
    public void setShelves(List<String> shelves) { this.shelves = shelves; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
