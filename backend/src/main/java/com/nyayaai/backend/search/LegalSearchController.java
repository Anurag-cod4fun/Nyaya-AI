package com.nyayaai.backend.search;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/search")
public class LegalSearchController {

    private final LegalSearchService searchService;

    public LegalSearchController(LegalSearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping
    public List<LegalSearchResult> search(
            @Valid @RequestBody SearchRequest request) {
        return searchService.search(request.query(), request.limit());
    }

    public record SearchRequest(
            @NotBlank String query,
            @Min(1) @Max(20) int limit
    ) {
    }
}