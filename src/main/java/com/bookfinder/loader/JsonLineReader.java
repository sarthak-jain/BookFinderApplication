package com.bookfinder.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JsonLineReader implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(JsonLineReader.class);
    private final BufferedReader reader;
    private final ObjectMapper mapper;

    public JsonLineReader(String filePath) throws IOException {
        this.mapper = new ObjectMapper();
        this.reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8),
                1024 * 1024
        );
        log.info("Opened JSON-lines file: {}", filePath);
    }

    public Stream<JsonNode> stream() {
        Iterator<JsonNode> iterator = new Iterator<>() {
            private String nextLine = null;
            private boolean finished = false;

            @Override
            public boolean hasNext() {
                if (finished) return false;
                if (nextLine != null) return true;
                try {
                    nextLine = reader.readLine();
                    if (nextLine == null) {
                        finished = true;
                        return false;
                    }
                    return true;
                } catch (IOException e) {
                    finished = true;
                    return false;
                }
            }

            @Override
            public JsonNode next() {
                if (!hasNext()) throw new NoSuchElementException();
                String line = nextLine;
                nextLine = null;
                try {
                    return mapper.readTree(line);
                } catch (Exception e) {
                    log.warn("Skipping malformed JSON line: {}", e.getMessage());
                    return mapper.createObjectNode();
                }
            }
        };

        Spliterator<JsonNode> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
