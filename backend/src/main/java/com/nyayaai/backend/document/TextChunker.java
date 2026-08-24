package com.nyayaai.backend.document;

import java.util.ArrayList;
import java.util.List;

public class TextChunker {

    private final int maxCharacters;

    public TextChunker(int maxCharacters) {
        if (maxCharacters < 100) {
            throw new IllegalArgumentException("Chunk size must be at least 100 characters");
        }
        this.maxCharacters = maxCharacters;
    }

    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        String normalizedText = text.replaceAll("\\s+", " ").trim();

        for (int start = 0; start < normalizedText.length();) {
            int end = Math.min(start + maxCharacters, normalizedText.length());
            if (end < normalizedText.length()) {
                int boundary = normalizedText.lastIndexOf(' ', end);
                if (boundary > start) {
                    end = boundary;
                }
            }
            chunks.add(normalizedText.substring(start, end));
            start = end;
            while (start < normalizedText.length() && normalizedText.charAt(start) == ' ') {
                start++;
            }
        }

        return chunks;
    }
}