package com.nyayaai.backend.document;

import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documents")
public class StatuteIngestionController {

    private final StatuteIngestionService ingestionService;

    public StatuteIngestionController(
            StatuteIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/statute/sections")
    public ResponseEntity<UUID> ingest(
            @Valid @RequestBody StatuteSectionRequest request) {

        return ResponseEntity.ok(
                ingestionService.ingest(request)
        );
    }
}