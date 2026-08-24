package com.nyayaai.backend.embedding;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class OllamaEmbeddingClient {

    private final RestClient restClient;

    public OllamaEmbeddingClient() {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:11434")
                .build();
    }

    public List<Double> embed(String text) {

        EmbedRequest request = new EmbedRequest(
                "bge-m3",
                text
        );

        EmbedResponse response = restClient.post()
                .uri("/api/embed")
                .body(request)
                .retrieve()
                .body(EmbedResponse.class);

        if (response == null ||
                response.embeddings() == null ||
                response.embeddings().isEmpty()) {

            throw new IllegalStateException(
                    "Ollama returned an empty embedding"
            );
        }

        return response.embeddings().getFirst();
    }

    private record EmbedRequest(
            String model,
            String input
    ) {}

    private record EmbedResponse(
            String model,
            List<List<Double>> embeddings
    ) {}
}