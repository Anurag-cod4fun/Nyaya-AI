package com.nyayaai.backend.embedding;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/embeddings")
public class EmbeddingController {

    private final OllamaEmbeddingClient embeddingClient;

    public EmbeddingController(OllamaEmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    @PostMapping("/test")
    public EmbeddingResponse test(@RequestBody EmbeddingRequest request) {

        List<Double> embedding =
                embeddingClient.embed(request.text());

        return new EmbeddingResponse(
                embedding.size(),
                embedding.subList(0, Math.min(5, embedding.size()))
        );
    }

    public record EmbeddingRequest(String text) {}

    public record EmbeddingResponse(
            int dimension,
            List<Double> firstValues
    ) {}
}