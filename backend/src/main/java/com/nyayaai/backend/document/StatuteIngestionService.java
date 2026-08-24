package com.nyayaai.backend.document;

import com.nyayaai.backend.chunk.ChunkType;
import com.nyayaai.backend.chunk.LegalChunk;
import com.nyayaai.backend.chunk.LegalChunkRepository;
import com.nyayaai.backend.chunk.LegalVectorRepository;
import com.nyayaai.backend.embedding.OllamaEmbeddingClient;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatuteIngestionService {

    private final DocumentRepository documentRepository;
    private final LegalChunkRepository chunkRepository;
    private final LegalVectorRepository vectorRepository;
    private final OllamaEmbeddingClient embeddingClient;

    public StatuteIngestionService(
            DocumentRepository documentRepository,
            LegalChunkRepository chunkRepository,
            LegalVectorRepository vectorRepository,
            OllamaEmbeddingClient embeddingClient) {

        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.vectorRepository = vectorRepository;
        this.embeddingClient = embeddingClient;
    }

    @Transactional
    public UUID ingest(StatuteSectionRequest request) {

        Document document = documentRepository
                .findById(request.documentId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Document not found: " + request.documentId()
                        ));

        LegalChunk chunk = new LegalChunk();

        chunk.setDocument(document);
        chunk.setChunkType(ChunkType.STATUTE_SECTION);
        chunk.setSection(request.section());
        chunk.setContent(request.content());

        LegalChunk savedChunk = chunkRepository.saveAndFlush(chunk);

        var embedding = embeddingClient.embed(request.content());

        if (embedding.size() != 1024) {
            throw new IllegalStateException(
                    "Unexpected embedding dimension: " + embedding.size()
            );
        }

        vectorRepository.saveEmbedding(
                savedChunk.getId(),
                embedding
        );

        return savedChunk.getId();
    }
}