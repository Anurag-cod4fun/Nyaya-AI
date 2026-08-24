package com.nyayaai.backend.document;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @Column(name = "document_id", nullable = false, length = 50)
    private String documentId;

    @Column(name = "case_name", nullable = false)
    private String caseName;

    private Integer year;

    private String citation;

    @Column(nullable = false, length = 100)
    private String court;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "authority_level", nullable = false)
    private Integer authorityLevel;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String topics;

    @Column(name = "legal_sections", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String legalSections;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Document() {
    }

    public static Document create(
            String documentId,
            String caseName,
            Integer year,
            String citation,
            String court,
            DocumentType documentType,
            String sourceType,
            Integer authorityLevel,
            String topics,
            String legalSections) {
        Document document = new Document();
        document.documentId = documentId;
        document.caseName = caseName;
        document.year = year;
        document.citation = citation;
        document.court = court;
        document.documentType = documentType;
        document.sourceType = sourceType;
        document.authorityLevel = authorityLevel;
        document.topics = topics;
        document.legalSections = legalSections;
        document.createdAt = LocalDateTime.now();
        return document;
    }

    public String getDocumentId() {
        return documentId;
    }

    // getters and setters
}