package com.nyayaai.backend.answer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class VertexAiAnswerClient {

    private final RestClient restClient;

    public VertexAiAnswerClient(
            @Value("${nyaya.vertex.url}") String url,
            @Value("${nyaya.vertex.access-token}") String accessToken) {
        this.restClient = RestClient.builder()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }

    public String generate(String prompt) {
        GenerateResponse response = restClient.post()
                .body(new GenerateRequest(
                        List.of(new Content("user", List.of(new Part(prompt))))))
                .retrieve()
                .body(GenerateResponse.class);

        if (response == null || response.candidates() == null || response.candidates().isEmpty()
                || response.candidates().getFirst().content() == null
                || response.candidates().getFirst().content().parts() == null
                || response.candidates().getFirst().content().parts().isEmpty()
                || response.candidates().getFirst().content().parts().getFirst().text() == null
                || response.candidates().getFirst().content().parts().getFirst().text().isBlank()) {
            throw new IllegalStateException("Vertex AI returned an empty legal answer");
        }

        return response.candidates().getFirst().content().parts().getFirst().text();
    }

    private record GenerateRequest(List<Content> contents) {
    }

    private record Content(String role, List<Part> parts) {
    }

    private record Part(String text) {
    }

    private record GenerateResponse(List<Candidate> candidates) {
    }

    private record Candidate(Content content) {
    }
}