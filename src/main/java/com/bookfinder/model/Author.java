package com.bookfinder.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Author")
public class Author {

    @Id
    private String authorId;

    private String role;

    public Author() {}

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
