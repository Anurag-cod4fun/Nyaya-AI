package com.nyayaai.backend.search;

import com.nyayaai.backend.embedding.OllamaEmbeddingClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LegalSearchService {

    private final OllamaEmbeddingClient embeddingClient;
    private final LegalSearchRepository searchRepository;

    public LegalSearchService(
            OllamaEmbeddingClient embeddingClient,
            LegalSearchRepository searchRepository) {
        this.embeddingClient = embeddingClient;
        this.searchRepository = searchRepository;
    }

    public List<LegalSearchResult> search(String query, int limit) {
        List<Double> embedding = embeddingClient.embed(query);

        if (embedding.size() != 1024) {
            throw new IllegalStateException(
                    "Unexpected embedding dimension: " + embedding.size()
            );
        }

        return searchRepository.findNearest(embedding.toString(), limit);
    }
}