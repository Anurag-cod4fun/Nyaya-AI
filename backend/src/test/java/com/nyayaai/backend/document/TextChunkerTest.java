package com.nyayaai.backend.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextChunkerTest {

    @Test
    void keepsChunksWithinConfiguredSizeAndPreservesText() {
        TextChunker chunker = new TextChunker(100);
        String text = "alpha ".repeat(80);

        List<String> chunks = chunker.chunk(text);

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().allMatch(chunk -> chunk.length() <= 100));
        assertEquals(text.trim().replaceAll("\\s+", " "), String.join(" ", chunks));
    }
}