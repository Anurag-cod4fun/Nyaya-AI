package com.nyayaai.backend.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfTextExtractor {

    public String extract(Path pdfPath) {

        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {

            PDFTextStripper stripper = new PDFTextStripper();

            return stripper.getText(document);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to extract text from PDF: " + pdfPath,
                    e
            );
        }
    }

    public List<String> extractPages(Path pdfPath) {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<String> pages = new ArrayList<>();

            for (int pageNumber = 1; pageNumber <= document.getNumberOfPages(); pageNumber++) {
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                pages.add(stripper.getText(document));
            }

            return pages;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to extract pages from PDF: " + pdfPath,
                    e
            );
        }
    }
}