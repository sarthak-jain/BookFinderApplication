package com.bookfinder.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Shelf")
public class Shelf {

    @Id
    private String name;

    public Shelf() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
