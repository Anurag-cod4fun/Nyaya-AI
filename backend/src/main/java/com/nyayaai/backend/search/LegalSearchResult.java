package com.nyayaai.backend.search;

import java.util.UUID;

public record LegalSearchResult(
        UUID chunkId,
        String documentId,
        String caseName,
        String court,
        String citation,
        String section,
        Integer pageNumber,
        String content,
        double distance
) {
}