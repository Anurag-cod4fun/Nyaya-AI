package com.nyayaai.backend.chunk;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LegalChunkRepository
        extends JpaRepository<LegalChunk, UUID> {

        void deleteByDocumentDocumentId(String documentId);
}