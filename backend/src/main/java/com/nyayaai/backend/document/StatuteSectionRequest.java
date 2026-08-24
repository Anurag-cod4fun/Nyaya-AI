package com.nyayaai.backend.document;

import jakarta.validation.constraints.NotBlank;

public record StatuteSectionRequest(

        @NotBlank
        String documentId,

        @NotBlank
        String section,

        @NotBlank
        String content
) {
}