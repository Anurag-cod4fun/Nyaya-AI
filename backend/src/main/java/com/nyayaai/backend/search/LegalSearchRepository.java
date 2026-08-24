package com.nyayaai.backend.search;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LegalSearchRepository {

    private final JdbcTemplate jdbcTemplate;

    public LegalSearchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<LegalSearchResult> findNearest(String embedding, int limit) {
        return jdbcTemplate.query(
                """
                SELECT c.chunk_id, c.document_id, d.case_name, d.court,
                       d.citation, c.section, c.page_number, c.content,
                       c.embedding <=> ?::vector AS distance
                FROM legal_chunks c
                JOIN documents d ON d.document_id = c.document_id
                WHERE c.embedding IS NOT NULL
                                    AND LENGTH(c.content) >= 100
                ORDER BY c.embedding <=> ?::vector
                LIMIT ?
                """,
                (resultSet, rowNumber) -> new LegalSearchResult(
                        resultSet.getObject("chunk_id", java.util.UUID.class),
                        resultSet.getString("document_id"),
                        resultSet.getString("case_name"),
                        resultSet.getString("court"),
                        resultSet.getString("citation"),
                        resultSet.getString("section"),
                        (Integer) resultSet.getObject("page_number"),
                        resultSet.getString("content"),
                        resultSet.getDouble("distance")
                ),
                embedding,
                embedding,
                limit
        );
    }
}