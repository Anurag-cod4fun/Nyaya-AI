package com.nyayaai.backend.answer;

import com.nyayaai.backend.search.LegalSearchResult;
import com.nyayaai.backend.search.LegalSearchService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LegalAnswerService {

    private final LegalSearchService searchService;
    private final VertexAiAnswerClient answerClient;

    public LegalAnswerService(
            LegalSearchService searchService,
            VertexAiAnswerClient answerClient) {
        this.searchService = searchService;
        this.answerClient = answerClient;
    }

    public LegalAnswer answer(String question, int limit) {
        List<LegalSearchResult> sources = searchService.search(question, limit);
        String prompt = buildPrompt(question, sources);
        return new LegalAnswer(answerClient.generate(prompt), sources);
    }

    private String buildPrompt(String question, List<LegalSearchResult> sources) {
        StringBuilder prompt = new StringBuilder("""
                You are Nyaya AI, an Indian legal research assistant.
                Answer only from the supplied sources. Do not invent law, facts, citations, or holdings.
                If the sources are insufficient, say so clearly. This is research assistance, not legal advice.
                Cite every material proposition using [S1], [S2], etc. and distinguish statutes from judgments.

                QUESTION:
                """).append(question).append("\n\nSOURCES:\n");

        for (int index = 0; index < sources.size(); index++) {
            LegalSearchResult source = sources.get(index);
            prompt.append("[S").append(index + 1).append("] ")
                    .append(source.caseName()).append("; ")
                    .append(source.court()).append("; ")
                    .append(source.citation()).append("; section=")
                    .append(source.section()).append("; page=")
                    .append(source.pageNumber()).append("\n")
                    .append(source.content()).append("\n\n");
        }
        return prompt.toString();
    }

    public record LegalAnswer(String answer, List<LegalSearchResult> sources) {
    }
}