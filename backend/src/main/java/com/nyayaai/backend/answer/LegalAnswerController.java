package com.nyayaai.backend.answer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
@RequestMapping("/api/ask")
public class LegalAnswerController {

    private final LegalAnswerService answerService;

    public LegalAnswerController(LegalAnswerService answerService) {
        this.answerService = answerService;
    }

    @PostMapping
    public LegalAnswerService.LegalAnswer ask(@Valid @RequestBody AskRequest request) {
        return answerService.answer(request.question(), request.limit());
    }

    public record AskRequest(
            @NotBlank String question,
            @Min(1) @Max(10) int limit) {
    }
}