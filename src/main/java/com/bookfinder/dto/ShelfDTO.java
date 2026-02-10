package com.bookfinder.dto;

public class ShelfDTO {
    private String name;
    private Integer count;

    public ShelfDTO() {}

    public ShelfDTO(String name, Integer count) {
        this.name = name;
        this.count = count;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
}
