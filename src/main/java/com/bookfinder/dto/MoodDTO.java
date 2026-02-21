package com.bookfinder.dto;

import java.util.List;

public class MoodDTO {
    private String key;
    private String name;
    private String description;
    private String color;
    private List<String> shelves;

    public MoodDTO() {}

    public MoodDTO(String key, String name, String description, String color, List<String> shelves) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.color = color;
        this.shelves = shelves;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public List<String> getShelves() { return shelves; }
    public void setShelves(List<String> shelves) { this.shelves = shelves; }
}
