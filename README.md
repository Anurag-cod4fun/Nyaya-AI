# Nyaya AI

Nyaya AI is a retrieval-augmented legal research assistant for Indian law. It ingests legal source documents, extracts and chunks their text, generates semantic embeddings, stores them in PostgreSQL with pgvector, retrieves relevant legal passages, and uses Vertex AI Gemini to generate grounded answers with citations.

> **Important:** Nyaya AI is an assistive legal-intelligence system. It does not replace legal officers, judicial interpretation, or final legal approval.

## Local Setup

Start PostgreSQL/pgvector:

```powershell
cd C:\Projects\nyaya-ai
docker compose up -d
```

Start or verify Ollama. If it is already running, `ollama serve` reports that
port 11434 is occupied; that is expected.

```powershell
ollama list
```

The list must include:

```text
bge-m3
```

Start the backend using an absolute POM path. This avoids the common mistake
of launching Maven from the workspace root, which has no `pom.xml`:

```powershell
mvn -f C:\Projects\nyaya-ai\backend\pom.xml org.springframework.boot:spring-boot-maven-plugin:4.1.0:run
```

The backend listens on port 8080.

Start the frontend:

```powershell
npm --prefix C:\Projects\nyaya-ai\frontend run dev
```

The frontend is normally available at `http://localhost:5173`.

## V1 Scope

The POC intentionally focuses on one narrow legal subdomain:

**Indian Contract Law → Breach of Contract + Damages**

Example question:

> A company terminated a contract before completion. What remedies might the other party have?

The intended research path is:

```text
Indian Contract Act
        ↓
Breach / Termination
        ↓
Compensation / Damages
        ↓
Stipulated Damages
        ↓
Liquidated Damages / Penalty
        ↓
Relevant Judicial Interpretation
        ↓
Supreme Court Cases
        ↓
High Court Cases
        ↓
Application to Present Facts
```

This focused scope allows the POC to demonstrate a complete legal-intelligence workflow while keeping the architecture extensible to other legal domains.

## Key Capabilities

- PDF ingestion
- Legal text extraction and chunking
- Document metadata management
- BGE-M3 semantic embeddings
- PostgreSQL + pgvector similarity search
- Retrieval-Augmented Generation (RAG)
- Vertex AI Gemini answer generation
- Source-grounded answers and citations
- Statute, Supreme Court, and High Court corpus support

## Architecture

```text
                         ┌──────────────────┐
                         │    Legal User    │
                         └────────┬─────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
                         │   Spring Boot    │
                         │       API        │
                         └────────┬─────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
                         │    RAG Engine    │
                         │ Query + Retrieval│
                         └────────┬─────────┘
                                  │
                                  ▼
                    ┌──────────────────────────┐
                    │ PostgreSQL + pgvector    │
                    │ Documents / Chunks       │
                    │ 1024-D Embeddings        │
                    │ Legal Metadata            │
                    └────────────┬─────────────┘
                                 │
                                 ▼
                         ┌──────────────────┐
                         │    Vertex AI     │
                         │   Gemini LLM     │
                         └────────┬─────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
                         │ Grounded Answer  │
                         │ + Citations      │
                         └──────────────────┘
```

### Ingestion Flow

```text
Legal PDF
   ↓
Text Extraction
   ↓
Legal Chunking
   ↓
BGE-M3 Embedding
   ↓
PostgreSQL + pgvector
```

### Question-Answering Flow

```text
User Question
      ↓
Query Embedding
      ↓
Vector Similarity Search
      ↓
Relevant Legal Chunks
      ↓
Context Construction
      ↓
Vertex AI Gemini
      ↓
Grounded Legal Answer
      ↓
Source Citations
```

## Corpus

The current V1 corpus contains:

| Source | Count | Authority Level |
|---|---:|---:|
| Indian Contract Act | 1 | 6 |
| Supreme Court judgments | 17 | 5 |
| High Court judgments | 18 | 4 |

The corpus covers the V1 topic of breach of contract and damages, including:

- Breach
- Termination
- Section 39
- Section 73
- Section 74
- Liquidated damages
- Penalty
- Reasonable compensation
- Mitigation
- Remoteness
- Stipulated damages

### Judgment Metadata

Each judgment has standardized metadata:

```json
{
  "documentId": "SC-007",
  "caseName": "The Authorised Officer, Central Bank of India v. Shanmugavelu",
  "year": 2024,
  "citation": "2024 INSC 80",
  "court": "SUPREME_COURT",
  "sourceType": "JUDGMENT",
  "authorityLevel": 5,
  "topics": [
    "SARFAESI",
    "forfeiture",
    "security-interest",
    "reasonable-compensation"
  ],
  "legalSections": [
    "73",
    "74"
  ]
}
```

Only judgments relevant to the selected V1 domain are retained in the corpus.

## Data Model

PostgreSQL is the primary persistence layer.

### `documents`

Stores document-level metadata:

```text
document_id
case_name
year
citation
document_type
court
source_type
authority_level
topics
legal_sections
created_at
```

### `legal_chunks`

Stores extracted legal text:

```text
chunk_id
document_id
page_number
paragraph_start
paragraph_end
content
embedding
created_at
chunk_type
section
metadata
```

Each chunk references its source document through `document_id`.

```text
Retrieved Chunk
      ↓
Source Document
      ↓
Case / Statute
      ↓
Citation + Metadata
```

This traceability is essential because the generated answer is not itself the legal authority.

## PostgreSQL + pgvector

PostgreSQL stores structured metadata and legal text. The pgvector extension provides vector storage and similarity search.

The current embedding column is:

```text
vector(1024)
```

BGE-M3 is used for both legal chunks and user queries.

```text
Legal Chunk → BGE-M3 → 1024-dimensional vector → pgvector
```

For a user question:

```text
User Question → BGE-M3 → Query Vector → pgvector → Relevant Chunks
```

This enables semantic retrieval rather than relying only on exact keyword matches.

## Embedding Model: BGE-M3

Nyaya AI uses `bge-m3` for embeddings.

The embedding model converts text into numerical representations that capture semantic relationships. It does not generate the final legal answer.

The architecture deliberately separates:

```text
Embedding Model ≠ Answer Generation Model
```

This allows the retrieval and generation layers to be independently replaced.

## LLM: Vertex AI Gemini

The final POC uses Google Vertex AI Gemini for answer generation.

The original local approach could use Ollama for local model execution. The final implementation keeps BGE-M3 locally for embeddings while using Vertex AI for answer generation, avoiding the need to run a large local LLM.

```text
                    Nyaya AI
                       │
          ┌────────────┴────────────┐
          │                         │
    BGE-M3 Embeddings          Vertex AI Gemini
          │                         │
      Retrieval                Generation
```

Vertex AI deployment must follow the applicable organization-approved identity, networking, security, and data-governance requirements when handling restricted information.

## RAG Pipeline

```text
User Question
      ↓
Query Embedding
      ↓
Vector Similarity Search
      ↓
Relevant Legal Chunks
      ↓
Context Construction
      ↓
Vertex AI Gemini
      ↓
Grounded Legal Answer
      ↓
Source Citations
```

For example:

```text
"What is reasonable compensation for breach of contract?"
```

can retrieve material related to Sections 73 and 74 and relevant judicial interpretations.

The key design principle is:

**Retrieve first → Generate second**

rather than asking the model to answer solely from pretrained knowledge.

## Authority Levels

The corpus preserves source authority as metadata.

| Source | Authority |
|---|---:|
| Indian Contract Act | 6 |
| Supreme Court | 5 |
| High Court | 4 |

Authority metadata allows future retrieval improvements such as authority-aware ranking.

Semantic similarity alone does not determine legal authority, so future retrieval can combine:

```text
Semantic Similarity
+
Legal Section Match
+
Topic Match
+
Authority Level
+
Source Type
```

## API

### Corpus Ingestion

```http
POST /api/ingestion/corpus
```

```powershell
Invoke-RestMethod -Method Post `
  http://localhost:8080/api/ingestion/corpus
```

### Semantic Search

```http
POST /api/search
```

```bash
curl --location 'http://localhost:8080/api/search' \
--header 'Content-Type: application/json' \
--data '{
  "query": "What is reasonable compensation for breach of contract?",
  "limit": 3
}'
```

### Grounded Legal Question

```http
POST /api/ask
```

```powershell
Invoke-RestMethod -Method Post `
  http://localhost:8080/api/ask `
  -ContentType 'application/json' `
  -Body '{"question":"What is reasonable compensation for breach of contract?","limit":5}'
```

The response contains the generated answer together with the retrieved source records used to ground the response.

## Local Setup

### Prerequisites

- Java 25
- Maven
- Docker
- PostgreSQL
- pgvector
- Ollama
- BGE-M3
- Google Cloud CLI
- Vertex AI access

### Start Infrastructure

```powershell
docker compose up -d
```

### Pull BGE-M3

```powershell
ollama pull bge-m3
```

Only the embedding model needs to run locally. The answer-generation model runs through Vertex AI.

### Configure Vertex AI

```powershell
$env:VERTEX_AI_URL = "https://aiplatform.googleapis.com/v1/projects/YOUR_PROJECT_ID/locations/global/publishers/google/models/gemini-2.5-flash:generateContent"

$env:VERTEX_AI_ACCESS_TOKEN = (gcloud auth print-access-token)
```

Refresh the access token when it expires.

### Start Backend

```powershell
cd backend
mvn spring-boot:run
```

The default corpus path is:

```text
../corpus
```

when the application is started from the `backend` directory.

Override it with:

```text
CORPUS_ROOT
```

if required.

## Repository Structure

```text
Nyaya-AI/
│
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── ...
│
├── corpus/
│   ├── statute/
│   ├── supreme-court/
│   └── high-court/
│
├── docker-compose.yml
├── README.md
└── ...
```

## Why RAG?

A generic LLM-based legal chatbot can produce plausible answers but may hallucinate provisions, confuse cases, provide no source traceability, or fail to distinguish authoritative sources.

Nyaya AI combines:

```text
Controlled Legal Corpus
        +
Structured Metadata
        +
Semantic Embeddings
        +
Vector Retrieval
        +
Authority Information
        +
LLM Reasoning
        +
Citations
```

The LLM is therefore treated as a reasoning and drafting component, not as the source of legal authority.

## Data Isolation

The target architecture is on-premise-ready.

The controlled application environment contains:

```text
Legal PDFs
    ↓
Text Extraction
    ↓
Chunking
    ↓
Embeddings
    ↓
PostgreSQL + pgvector
    ↓
RAG Retrieval
```

The external AI layer is separated from the legal corpus. Only approved context required for generation should cross that boundary, subject to the deployment's security and data-governance policies.

## Recommended Demo

1. Ingest the legal corpus.
2. Demonstrate semantic search.
3. Ask a breach-of-contract question.
4. Show retrieved statutory and judicial passages.
5. Generate the grounded answer.
6. Show citations and source metadata.

Example:

```text
A company terminated a contract before completion.
What remedies might the other party have?
```

Expected research path:

```text
Section 39
    ↓
Section 73
    ↓
Section 74
    ↓
Relevant Supreme Court decisions
    ↓
Relevant High Court decisions
    ↓
Application to facts
```

## V1 Limitations

The current POC does not yet provide:

- Full Indian legal coverage
- Complete licensed legal-database integration
- Authentication and authorization
- Human approval workflow
- Hybrid keyword + vector retrieval
- Advanced reranking
- Automated conflict detection
- Production-grade multilingual research
- Comprehensive automated evaluation benchmarks

These are extension points for future versions.

## Future Enhancements

- Authority-aware retrieval ranking
- Hybrid BM25 + vector search
- Cross-encoder reranking
- Legal issue extraction
- Precedent and conflict detection
- Structured legal opinion generation
- Human review and approval workflow
- Role-based access control
- Complete audit trail
- Multilingual Indian-language research
- Automated retrieval and grounding evaluation
- Additional statutes and legal domains
- Secure government repository adapters
- Frontend legal research workspace
- Production observability and monitoring

## Design Principle

> **The AI assists legal research; the source material and authorized human officer remain the authority.**

Nyaya AI retrieves relevant law and precedent, provides traceable supporting material, and assists with drafting. Final legal interpretation, review, and approval remain with the authorized legal officer.

## Project Status

**POC Complete — V1**

Current focus:

**Indian Contract Law → Breach of Contract + Damages**

The architecture is modular so that additional statutes, judgments, and legal domains can be added without redesigning the core RAG pipeline.
