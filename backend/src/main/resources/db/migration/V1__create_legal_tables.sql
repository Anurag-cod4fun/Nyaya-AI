CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE documents (
    document_id VARCHAR(50) PRIMARY KEY,
    case_name TEXT NOT NULL,
    year INTEGER,
    citation TEXT,
    court VARCHAR(100) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    authority_level INTEGER NOT NULL,
    topics JSONB,
    legal_sections JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE legal_chunks (
    chunk_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    document_id VARCHAR(50) NOT NULL,

    page_number INTEGER,
    paragraph_start INTEGER,
    paragraph_end INTEGER,

    content TEXT NOT NULL,

    embedding VECTOR(768),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_legal_chunk_document
        FOREIGN KEY (document_id)
        REFERENCES documents(document_id)
        ON DELETE CASCADE
);