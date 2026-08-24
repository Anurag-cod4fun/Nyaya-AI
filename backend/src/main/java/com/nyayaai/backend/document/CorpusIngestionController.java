package com.nyayaai.backend.document;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingestion")
public class CorpusIngestionController {

    private final CorpusIngestionService ingestionService;

    public CorpusIngestionController(CorpusIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/corpus")
    public CorpusIngestionService.IngestionResult ingestCorpus() {
        return ingestionService.ingestAll();
    }
}