package com.nyayaai.backend.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PdfTextExtractorTest {

    @Test
    void shouldExtractTextFromJudgment() {

        PdfTextExtractor extractor = new PdfTextExtractor();

        String text = extractor.extract(
                Path.of("../corpus/judgments/supreme-court/SC-001/PDF/sc_01.pdf")
        );

        assertNotNull(text);
        assertFalse(text.isBlank());

        System.out.println(
                text.substring(0, Math.min(1000, text.length()))
        );
    }
}