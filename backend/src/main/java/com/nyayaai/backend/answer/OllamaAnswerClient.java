package com.nyayaai.backend.answer;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class OllamaAnswerClient {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("http://localhost:11434")
            .build();

    public String generate(String prompt) {
        GenerateResponse response = restClient.post()
                .uri("/api/generate")
                .body(new GenerateRequest(
                    "llama3.1:8b",
                    prompt,
                    false,
                    Map.of("num_predict", 384, "temperature", 0.1)))
                .retrieve()
                .body(GenerateResponse.class);

        if (response == null || response.response() == null || response.response().isBlank()) {
            throw new IllegalStateException("Ollama returned an empty legal answer");
        }
        return response.response();
    }

        private record GenerateRequest(
            String model,
            String prompt,
            boolean stream,
            Map<String, Object> options) {
    }

    private record GenerateResponse(String response) {
    }
}