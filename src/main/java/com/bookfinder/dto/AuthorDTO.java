package com.bookfinder.dto;

public class AuthorDTO {
    private String authorId;
    private String name;
    private String role;

    public AuthorDTO() {}

    public AuthorDTO(String authorId, String name, String role) {
        this.authorId = authorId;
        this.name = name;
        this.role = role;
    }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
