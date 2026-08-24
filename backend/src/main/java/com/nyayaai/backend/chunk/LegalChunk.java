package com.nyayaai.backend.chunk;

import com.nyayaai.backend.document.Document;
import java.util.UUID;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "legal_chunks")
public class LegalChunk {

    @Id
    @GeneratedValue
    @Column(name = "chunk_id", nullable = false)
    private UUID chunkId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Enumerated(EnumType.STRING)
    @Column(name = "chunk_type", nullable = false)
    private ChunkType chunkType;

    @Column(length = 50)
    private String section;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "paragraph_start")
    private Integer paragraphStart;

    @Column(name = "paragraph_end")
    private Integer paragraphEnd;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    /*
     * We will NOT map the pgvector column yet.
     *
     * We'll handle it explicitly when we implement
     * vector persistence/retrieval.
     */

    public LegalChunk() {
    }

    // getters and setters
    public void setDocument(Document document) {
        this.document = document;
    }

    public void setChunkType(ChunkType chunkType) {
        this.chunkType = chunkType;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public UUID getId() {
        return chunkId;
    }
}