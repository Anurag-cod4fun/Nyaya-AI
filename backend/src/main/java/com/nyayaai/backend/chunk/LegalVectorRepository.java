package com.nyayaai.backend.chunk;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class LegalVectorRepository {

    private final JdbcTemplate jdbcTemplate;

    public LegalVectorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean hasEmbeddings(String documentId) {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM legal_chunks
                    WHERE document_id = ?
                      AND embedding IS NOT NULL
                )
                """,
                Boolean.class,
                documentId
        );
        return Boolean.TRUE.equals(exists);
    }

    public void saveEmbedding(UUID chunkId, List<Double> embedding) {

        String vector = embedding.toString()
                .replace("[", "[")
                .replace("]", "]");

        jdbcTemplate.update(
                """
                UPDATE legal_chunks
                SET embedding = ?::vector
                WHERE chunk_id = ?
                """,
                vector,
                chunkId
        );
    }
}