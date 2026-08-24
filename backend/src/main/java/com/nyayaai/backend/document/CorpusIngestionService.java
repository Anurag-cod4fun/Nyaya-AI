package com.nyayaai.backend.document;

import tools.jackson.databind.ObjectMapper;
import com.nyayaai.backend.chunk.ChunkType;
import com.nyayaai.backend.chunk.LegalChunk;
import com.nyayaai.backend.chunk.LegalChunkRepository;
import com.nyayaai.backend.chunk.LegalVectorRepository;
import com.nyayaai.backend.embedding.OllamaEmbeddingClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Service
public class CorpusIngestionService {

    private final DocumentRepository documentRepository;
    private final LegalChunkRepository chunkRepository;
    private final LegalVectorRepository vectorRepository;
    private final OllamaEmbeddingClient embeddingClient;
    private final PdfTextExtractor pdfTextExtractor;
    private final ObjectMapper objectMapper;
    private final Path corpusRoot;
    private final TextChunker textChunker = new TextChunker(1200);

    public CorpusIngestionService(
            DocumentRepository documentRepository,
            LegalChunkRepository chunkRepository,
            LegalVectorRepository vectorRepository,
            OllamaEmbeddingClient embeddingClient,
            PdfTextExtractor pdfTextExtractor,
            ObjectMapper objectMapper,
            @Value("${nyaya.corpus-root}") String corpusRoot) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.vectorRepository = vectorRepository;
        this.embeddingClient = embeddingClient;
        this.pdfTextExtractor = pdfTextExtractor;
        this.objectMapper = objectMapper;
        this.corpusRoot = resolveCorpusRoot(corpusRoot);
    }

    @Transactional
    public IngestionResult ingestAll() {
        int documents = 0;
        int chunks = 0;
        int skipped = 0;

        try {
            Path judgmentsRoot = corpusRoot.resolve("judgments");
            if (!Files.isDirectory(judgmentsRoot)) {
                throw new IllegalStateException("Judgments corpus directory not found: " + judgmentsRoot);
            }
            try (Stream<Path> metadataFiles = Files.walk(judgmentsRoot)) {
                for (Path metadataFile : metadataFiles
                        .filter(path -> path.getFileName().toString().equals("metadata.json"))
                        .toList()) {
                    CorpusMetadata metadata = objectMapper.readValue(
                            metadataFile.toFile(), CorpusMetadata.class);
                    Path documentDirectory = metadataFile.getParent();
                    Path pdf = findPdf(documentDirectory.resolve("PDF"));
                    Document document = saveDocument(metadata);
                    if (vectorRepository.hasEmbeddings(document.getDocumentId())) {
                        skipped++;
                        continue;
                    }
                    chunkRepository.deleteByDocumentDocumentId(document.getDocumentId());
                    chunks += ingestPdf(document, pdf, ChunkType.JUDGMENT_PARAGRAPH);
                    documents++;
                }
            }

            Path statutesRoot = corpusRoot.resolve("statues");
            if (Files.exists(statutesRoot)) {
                try (Stream<Path> statuteFiles = Files.walk(statutesRoot)) {
                    for (Path pdf : statuteFiles
                            .filter(path -> path.toString().toLowerCase().endsWith(".pdf"))
                            .toList()) {
                            IngestionResult result = ingestStatute(pdf);
                            chunks += result.chunks();
                            documents += result.documents();
                            skipped += result.skipped();
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to ingest corpus at " + corpusRoot, e);
        }

        return new IngestionResult(documents, chunks, skipped);
    }

    private Path resolveCorpusRoot(String configuredRoot) {
        Path configuredPath = Path.of(configuredRoot).toAbsolutePath().normalize();
        if (Files.isDirectory(configuredPath)) {
            return configuredPath;
        }

        Path workspacePath = Path.of("corpus").toAbsolutePath().normalize();
        if (Files.isDirectory(workspacePath)) {
            return workspacePath;
        }

        throw new IllegalStateException(
                "Corpus directory not found. Checked: " + configuredPath + " and " + workspacePath);
    }

    private Document saveDocument(CorpusMetadata metadata) throws IOException {
        String topics = objectMapper.writeValueAsString(metadata.topics());
        String legalSections = objectMapper.writeValueAsString(metadata.legalSections());
        Document document = Document.create(
                metadata.documentId(), metadata.caseName(), metadata.year(), metadata.citation(),
                metadata.court(), DocumentType.JUDGMENT, metadata.sourceType(),
                metadata.authorityLevel(), topics, legalSections);
        Document savedDocument = documentRepository.save(document);
        return savedDocument;
    }

    private IngestionResult ingestStatute(Path pdf) throws IOException {
        String section = pdf.getFileName().toString()
                .replace("section-", "")
                .replace(".pdf", "");
        String documentId = "STATUTE-INDIAN-CONTRACT-ACT-1872-" + section;
        Document document = documentRepository.findById(documentId).orElseGet(() ->
                documentRepository.save(Document.create(
                        documentId, "Indian Contract Act, 1872, Section " + section,
                        1872, null, "INDIA", DocumentType.STATUTE, "STATUTE", 5,
                        "[\"indian-contract-act-1872\"]", "[\"" + section + "\"]")));
        if (vectorRepository.hasEmbeddings(document.getDocumentId())) {
            return new IngestionResult(0, 0, 1);
        }
        chunkRepository.deleteByDocumentDocumentId(document.getDocumentId());
        return new IngestionResult(1, ingestPdf(document, pdf, ChunkType.STATUTE_SECTION, section), 0);
    }

    private int ingestPdf(Document document, Path pdf, ChunkType chunkType) {
        return ingestPdf(document, pdf, chunkType, null);
    }

    private int ingestPdf(Document document, Path pdf, ChunkType chunkType, String section) {
        int chunks = 0;
        List<String> pages = pdfTextExtractor.extractPages(pdf);
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            for (String content : textChunker.chunk(pages.get(pageIndex))) {
                LegalChunk chunk = new LegalChunk();
                chunk.setDocument(document);
                chunk.setChunkType(chunkType);
                chunk.setSection(section);
                chunk.setPageNumber(pageIndex + 1);
                chunk.setContent(content);
                LegalChunk savedChunk = chunkRepository.saveAndFlush(chunk);
                vectorRepository.saveEmbedding(savedChunk.getId(), embeddingClient.embed(content));
                chunks++;
            }
        }
        return chunks;
    }

    private Path findPdf(Path pdfDirectory) throws IOException {
        try (Stream<Path> files = Files.list(pdfDirectory)) {
            return files.filter(path -> path.toString().toLowerCase().endsWith(".pdf"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No PDF found in " + pdfDirectory));
        }
    }

    public record IngestionResult(int documents, int chunks, int skipped) {
    }

    private record CorpusMetadata(
            String documentId,
            String caseName,
            Integer year,
            String citation,
            String court,
            String sourceType,
            Integer authorityLevel,
            List<String> topics,
            List<String> legalSections) {
    }
}