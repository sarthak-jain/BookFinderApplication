package com.bookfinder.dto;

public class AuthorDTO {
    private String authorId;
    private String role;

    public AuthorDTO() {}

    public AuthorDTO(String authorId, String role) {
        this.authorId = authorId;
        this.role = role;
    }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
