# Nyaya AI Backend RAG: Deep Technical Documentation

## 1. Purpose

Nyaya AI is a retrieval-augmented generation (RAG) system for Indian legal
research. Its job is to answer questions using a local corpus of:

- Supreme Court judgments
- High Court judgments
- Indian statute section PDFs

The backend does not ask Gemini to answer from general knowledge alone. It
first retrieves relevant passages from the indexed legal corpus and then sends
those passages, together with the user question, to Gemini. The generated
answer is returned together with the source records used for the answer.

The current architecture uses two different model responsibilities:

```text
Legal PDFs and metadata
        |
        v
PDF extraction and chunking
        |
        v
Ollama bge-m3 embedding model
        |
        v
PostgreSQL + pgvector
        |
User question --> bge-m3 embedding --> nearest-neighbor retrieval
                                                |
                                                v
                                  Vertex AI Gemini 2.5 Flash
                                                |
                                                v
                                      Grounded cited answer
```

## 2. Technology stack

| Layer | Technology | Responsibility |
|---|---|---|
| Application | Spring Boot 4.1.0 | HTTP API, dependency injection, transactions |
| Language | Java 25 | Backend implementation |
| Relational database | PostgreSQL 17 | Documents, chunks, metadata |
| Vector database | pgvector extension | 1024-dimensional embeddings and similarity search |
| PDF parsing | Apache PDFBox 3.0.8 | Extract text from PDFs |
| Embedding model | Ollama `bge-m3` | Converts text into vectors |
| Answer model | Vertex AI Gemini 2.5 Flash | Produces the final grounded answer |
| Schema migration | Flyway | Creates and evolves database tables |
| Frontend | React + Vite | Browser-based research workspace |

## 3. Source code map

```text
backend/src/main/java/com/nyayaai/backend/
|
+- document/
|  +- CorpusIngestionController.java
|  +- CorpusIngestionService.java
|  +- Document.java
|  +- DocumentRepository.java
|  +- DocumentType.java
|  +- PdfTextExtractor.java
|  +- TextChunker.java
|  +- StatuteIngestionController.java
|  +- StatuteIngestionService.java
|  +- StatuteSectionRequest.java
|
+- chunk/
|  +- LegalChunk.java
|  +- LegalChunkRepository.java
|  +- LegalVectorRepository.java
|  +- ChunkType.java
|
+- embedding/
|  +- OllamaEmbeddingClient.java
|  +- EmbeddingController.java
|
+- search/
|  +- LegalSearchController.java
|  +- LegalSearchService.java
|  +- LegalSearchRepository.java
|  +- LegalSearchResult.java
|
+- answer/
   +- LegalAnswerController.java
   +- LegalAnswerService.java
   +- VertexAiAnswerClient.java
```

The frontend is in `frontend/src/` and calls the answer endpoint from the
browser.

## 4. Two-model design

### 4.1 Embedding model: bge-m3

`bge-m3` is used for semantic representation, not answer generation. The
client is `OllamaEmbeddingClient`.

Its request is sent to the local Ollama HTTP API:

```json
{
  "model": "bge-m3",
  "input": "text to embed"
}
```

The Ollama response contains a list of vectors. The code selects the first
vector:

```java
return response.embeddings().getFirst();
```

The service expects 1024 values:

```java
if (embedding.size() != 1024) {
    throw new IllegalStateException(
            "Unexpected embedding dimension: " + embedding.size()
    );
}
```

This dimension must match the PostgreSQL column definition. The database
migration history starts with `VECTOR(768)` in V1 and changes it to
`VECTOR(1024)` in V2. The final schema is therefore 1024-dimensional.

### 4.2 Answer model: Vertex AI Gemini

`VertexAiAnswerClient` sends the constructed RAG prompt to Vertex AI using the
Google REST contract:

```http
POST https://aiplatform.googleapis.com/v1/projects/{project}/locations/global/publishers/google/models/gemini-2.5-flash:generateContent
Authorization: Bearer {oauth-access-token}
Content-Type: application/json
```

The request body is represented by Java records:

```java
private record GenerateRequest(List<Content> contents) {}
private record Content(String role, List<Part> parts) {}
private record Part(String text) {}
```

The resulting JSON is equivalent to:

```json
{
  "contents": [
    {
      "role": "user",
      "parts": [
        {
          "text": "the RAG prompt"
        }
      ]
    }
  ]
}
```

The response is parsed with matching records:

```java
private record GenerateResponse(List<Candidate> candidates) {}
private record Candidate(Content content) {}
```

The answer is extracted from:

```text
candidates[0].content.parts[0].text
```

If Vertex returns no candidate, no content, or blank text, the client throws an
`IllegalStateException` instead of returning an empty answer.

## 5. Corpus layout

The checked-in corpus is organized as follows:

```text
corpus/
+- judgments/
|  +- supreme-court/
|  |  +- SC-001/
|  |     +- metadata.json
|  |     +- PDF/sc_01.pdf
|  +- high-court/
|     +- HC-001/
|        +- metadata.json
|        +- PDF/hc-01.pdf
|
+- statues/
   +- indian-contract-act-1872/
      +- section-39.pdf
      +- section-55.pdf
      +- section-62.pdf
      +- section-63.pdf
      +- section-73.pdf
      +- section-74.pdf
      +- section-75.pdf
```

The directory is named `statues` in the current repository. That spelling is
part of the current implementation and should not be changed without also
updating the ingester.

A judgment metadata file contains fields such as:

```json
{
  "documentId": "SC-001",
  "caseName": "Maharashtra State Electricity Board v. Sterilite Industries (India)",
  "year": 2001,
  "citation": "(2001) 8 SCC 482",
  "court": "SUPREME_COURT",
  "sourceType": "JUDGMENT",
  "authorityLevel": 5,
  "topics": ["breach-of-contract", "damages"],
  "legalSections": ["73", "74"]
}
```

## 6. Database schema

### 6.1 documents

The `documents` table stores document-level source information:

- `document_id`: stable corpus identifier such as `SC-001`
- `case_name`: case or statute display name
- `year`: publication or decision year
- `citation`: legal citation
- `court`: court or jurisdiction
- `document_type`: `JUDGMENT` or `STATUTE`
- `source_type`: corpus source classification
- `authority_level`: numeric authority ranking, currently stored but not yet
  used in retrieval ordering
- `topics`: JSONB topic array
- `legal_sections`: JSONB section array
- `created_at`: insertion timestamp

### 6.2 legal_chunks

The `legal_chunks` table stores searchable units:

- `chunk_id`: UUID primary key
- `document_id`: foreign key to `documents`
- `chunk_type`: `JUDGMENT_PARAGRAPH` or `STATUTE_SECTION`
- `section`: statute section when available
- `page_number`: source PDF page number
- `paragraph_start`, `paragraph_end`: reserved for future paragraph tracking
- `content`: normalized text
- `metadata`: JSONB extension point
- `embedding`: pgvector column with 1024 dimensions
- `created_at`: insertion timestamp

The foreign key uses `ON DELETE CASCADE`, so deleting a document also deletes
its chunks.

### 6.3 Flyway history

Migrations are applied in order:

1. `V1__create_legal_tables.sql`
   - Enables pgvector.
   - Creates the two base tables.
2. `V2__change_embedding_dimension.sql`
   - Changes the vector from 768 to 1024 dimensions.
3. `V3__add_legal_chunk_metadata.sql`
   - Adds chunk type, section, and metadata.
4. `V4__add_document_type.sql`
   - Adds document type to documents.

Flyway runs during Spring Boot startup. Hibernate uses `ddl-auto: validate`,
so Hibernate checks the schema but does not create or modify tables.

## 7. Ingestion pipeline

The endpoint is:

```http
POST /api/ingestion/corpus
```

No request body is required.

### 7.1 Corpus root resolution

`CorpusIngestionService` receives:

```yaml
nyaya:
  corpus-root: ${CORPUS_ROOT:../corpus}
```

The constructor calls `resolveCorpusRoot`. It first checks the configured
path. If that path is not a directory, it checks a workspace-relative `corpus`
path. This supports starting the application from either the backend directory
or the workspace root.

If neither path exists, the service fails with a message naming both checked
paths.

### 7.2 Judgment ingestion

The service walks `corpus/judgments` and selects every file named
`metadata.json`:

```java
try (Stream<Path> metadataFiles = Files.walk(judgmentsRoot)) {
    for (Path metadataFile : metadataFiles
            .filter(path -> path.getFileName().toString().equals("metadata.json"))
            .toList()) {
        ...
    }
}
```

For each metadata file it:

1. Deserializes JSON into the private `CorpusMetadata` record.
2. Locates the first PDF in the sibling `PDF` directory.
3. Creates or saves a `Document` entity using the metadata.
4. Checks whether that document already has an embedding.
5. Skips fully indexed documents.
6. Extracts PDF pages.
7. Chunks each page.
8. Persists each chunk.
9. Embeds each chunk with `bge-m3`.
10. Updates the chunk's vector column.

### 7.3 Statute ingestion

Statute files do not have metadata JSON. The service derives their identity
from the filename:

```text
section-74.pdf
```

becomes:

```text
STATUTE-INDIAN-CONTRACT-ACT-1872-74
```

It creates a synthetic document with:

- name: `Indian Contract Act, 1872, Section 74`
- year: `1872`
- court: `INDIA`
- type: `STATUTE`
- source type: `STATUTE`
- authority level: `5`
- section: `74`

The PDF is then processed using the same page extraction, chunking, embedding,
and vector persistence path.

### 7.4 Idempotency

Repeated calls do not re-embed completed documents. The check is implemented
in `LegalVectorRepository.hasEmbeddings`:

```sql
SELECT EXISTS (
    SELECT 1
    FROM legal_chunks
    WHERE document_id = ?
      AND embedding IS NOT NULL
)
```

The response contains:

```json
{
  "documents": 0,
  "chunks": 0,
  "skipped": 43
}
```

This means the corpus was scanned but all 43 documents already had embeddings.
The check is document-level. If a document has some embedded chunks and some
missing chunks, the current code treats it as indexed; incomplete-job recovery
should be improved later.

## 8. PDF extraction

`PdfTextExtractor` uses PDFBox:

```java
try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
    PDFTextStripper stripper = new PDFTextStripper();
    return stripper.getText(document);
}
```

For RAG ingestion, `extractPages` is used instead. It loops from page 1 to the
PDF page count and configures the stripper for one page at a time:

```java
for (int pageNumber = 1;
     pageNumber <= document.getNumberOfPages();
     pageNumber++) {
    stripper.setStartPage(pageNumber);
    stripper.setEndPage(pageNumber);
    pages.add(stripper.getText(document));
}
```

Page-aware extraction is important because search results can return the source
page number to the user and to Gemini.

This works for text-based PDFs. Scanned image-only PDFs require OCR, which is
not implemented yet.

## 9. Text chunking

`TextChunker` currently receives a maximum character size of 1200 from the
corpus ingester.

The algorithm:

1. Collapses all whitespace with `\\s+`.
2. Trims the page text.
3. Takes at most 1200 characters.
4. If possible, moves the boundary backward to the last space.
5. Skips spaces before the next chunk.

This avoids splitting ordinary words, but it has no overlap:

```text
chunk 1 ends here
chunk 2 starts here
```

Legal reasoning can cross chunk boundaries, so an overlap of approximately
100-200 characters is a recommended future improvement. Paragraph-aware or
section-aware splitting would also improve legal citations.

Empty page text currently produces no chunks because the chunk loop has no
iterations when normalized text length is zero.

## 10. Chunk and vector persistence

A `LegalChunk` is first written with JPA:

```java
LegalChunk savedChunk = chunkRepository.saveAndFlush(chunk);
```

`saveAndFlush` is important because the database must assign and persist the
UUID before the separate JDBC update can target it.

The vector is stored by `LegalVectorRepository`:

```sql
UPDATE legal_chunks
SET embedding = ?::vector
WHERE chunk_id = ?
```

The Java list is converted into PostgreSQL vector literal syntax using its
`toString()` representation, for example:

```text
[0.012, -0.041, 0.208, ...]
```

The vector column is deliberately not mapped as a JPA field. This avoids
requiring a pgvector-specific Hibernate type and keeps vector writes in the
explicit JDBC repository.

## 11. Semantic search pipeline

The endpoint is:

```http
POST /api/search
Content-Type: application/json
```

Request:

```json
{
  "query": "What is reasonable compensation for breach of contract?",
  "limit": 5
}
```

### 11.1 Controller validation

`LegalSearchController.SearchRequest` validates:

- `query` must not be blank
- `limit` must be at least 1
- `limit` must be at most 20

### 11.2 Query embedding

`LegalSearchService` sends the query to `OllamaEmbeddingClient`, verifies 1024
dimensions, and converts the list to a vector literal.

### 11.3 pgvector query

The repository joins chunks to documents and ranks by cosine distance:

```sql
SELECT c.chunk_id, c.document_id, d.case_name, d.court,
       d.citation, c.section, c.page_number, c.content,
       c.embedding <=> ?::vector AS distance
FROM legal_chunks c
JOIN documents d ON d.document_id = c.document_id
WHERE c.embedding IS NOT NULL
  AND LENGTH(c.content) >= 100
ORDER BY c.embedding <=> ?::vector
LIMIT ?
```

The `<=>` operator is pgvector cosine distance. Lower values are more similar.
It is a distance, not a percentage or probability.

The `LENGTH(c.content) >= 100` condition prevents contextless fragments such as
`for the breach of contract` from becoming sources. This improves answer
quality without changing already stored data.

### 11.4 Search response

Each `LegalSearchResult` contains:

- `chunkId`
- `documentId`
- `caseName`
- `court`
- `citation`
- `section`
- `pageNumber`
- `content`
- `distance`

The current search is semantic-only. It does not yet filter or boost by court,
document type, authority level, section, year, or keyword overlap.

## 12. Grounded answer pipeline

The endpoint is:

```http
POST /api/ask
Content-Type: application/json
```

Request:

```json
{
  "question": "What is reasonable compensation for breach of contract?",
  "limit": 3
}
```

`LegalAnswerController` validates the question and limits the maximum source
count to 10.

`LegalAnswerService.answer` executes three steps:

```java
List<LegalSearchResult> sources = searchService.search(question, limit);
String prompt = buildPrompt(question, sources);
return new LegalAnswer(answerClient.generate(prompt), sources);
```

### 12.1 Prompt construction

The prompt instructs Gemini to:

- act as an Indian legal research assistant
- answer only from supplied sources
- avoid invented law, facts, citations, and holdings
- state when the sources are insufficient
- explain that the output is research assistance, not legal advice
- cite material propositions as `[S1]`, `[S2]`, and so on
- distinguish statutes and judgments

Each retrieved source is appended with a stable prompt-local source number:

```text
[S1] case name; court; citation; section=...; page=...
source passage
```

The source number is local to one answer request. It is not the database
`chunkId`.

### 12.2 Gemini response

Gemini returns candidates. The client takes the first candidate's first text
part. The API returns:

```json
{
  "answer": "... [S1] ... [S2] ...",
  "sources": [
    {
      "documentId": "SC-018",
      "caseName": "...",
      "citation": "(2022) 2 SCC 382",
      "pageNumber": 11,
      "content": "...",
      "distance": 0.269
    }
  ]
}
```

The backend does not verify that every citation marker in Gemini's text points
to a valid source. It supplies the sources and prompt instruction, but citation
validation is a future hardening task.

## 13. Configuration

Current configuration lives in `backend/src/main/resources/application.yml`.
Important properties:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/nyaya
    username: nyaya
    password: nyaya
  flyway:
    enabled: true
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false

nyaya:
  corpus-root: ${CORPUS_ROOT:../corpus}
  vertex:
    url: ${VERTEX_AI_URL:...}
    access-token: ...
```

The current file has a directly pasted OAuth access token. That token is
sensitive and temporary. It should be revoked/rotated and replaced with:

```yaml
access-token: ${VERTEX_AI_ACCESS_TOKEN:}
```

Then set it only in the process environment:

```powershell
$env:VERTEX_AI_ACCESS_TOKEN = (gcloud auth print-access-token)
```

Never commit a bearer token, service-account private key, or API secret to the
repository.

## 14. Local startup

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

## 15. API examples

Health:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Search:

```powershell
$body = @{
  query = "What is reasonable compensation for breach of contract?"
  limit = 5
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/search" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body
```

Ask:

```powershell
$body = @{
  question = "What is reasonable compensation for breach of contract?"
  limit = 3
} | ConvertTo-Json

$response = Invoke-RestMethod `
  -Uri "http://localhost:8080/api/ask" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body

$response | ConvertTo-Json -Depth 10
```

Corpus ingestion is idempotent and should be run only when the corpus is new
or the index is incomplete:

```powershell
Invoke-RestMethod -Method Post http://localhost:8080/api/ingestion/corpus
```

## 16. Frontend integration

The React frontend uses:

```javascript
fetch('http://localhost:8080/api/ask', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ question, limit }),
})
```

The backend answer controller permits local Vite origins:

```java
@CrossOrigin(origins = {
    "http://localhost:5173",
    "http://localhost:5174"
})
```

This is why Postman can work while the browser reports `Failed to fetch` when
CORS is absent: Postman does not enforce browser same-origin rules.

## 17. Tests

Current backend tests include:

- `PdfTextExtractorTest`: extracts text from a corpus judgment PDF.
- `TextChunkerTest`: verifies chunk size and text preservation.
- `LegalSearchServiceTest`: verifies unexpected embedding dimensions are
  rejected before database search.

Run them with:

```powershell
mvn -f C:\Projects\nyaya-ai\backend\pom.xml test
```

The frontend production build is checked with:

```powershell
npm --prefix C:\Projects\nyaya-ai\frontend run build
```

## 18. What is currently strong

- The indexing and answer responsibilities are separated.
- The corpus is persisted instead of reprocessed for every question.
- Search returns source records along with the answer.
- Gemini is explicitly instructed to ground claims in retrieved sources.
- The vector dimension is checked in application code.
- Database schema is managed with Flyway.
- Repeated corpus ingestion skips already indexed documents.
- Short contextless chunks are excluded from retrieval.
- The browser workflow has loading, error, and source display states.

## 19. Current limitations and risks

### Security

- The current `application.yml` contains a pasted bearer token and must be
  treated as exposed.
- The ingestion endpoint has no authentication or authorization.
- Database credentials are development defaults in Docker configuration.
- CORS is configured for local origins only.

### Retrieval quality

- Retrieval is vector-only; there is no BM25 or PostgreSQL full-text search.
- `authority_level` is stored but not used in ranking.
- Court and document type are returned but not used as filters.
- Chunking has no overlap and can cut legal sentences at page boundaries.
- Sources are not deduplicated by document or passage.
- The short-fragment rule is character based rather than sentence based.

### Ingestion reliability

- Ingestion is synchronous HTTP work and can take a long time.
- Each chunk makes a separate Ollama request.
- There is no progress endpoint or background job.
- There is no retry/backoff for Ollama failures.
- The idempotency check does not detect partially embedded documents.
- Scanned PDFs need OCR support.
- There is no content hash/version to detect changed PDFs.

### Answer quality

- Gemini citation markers are not programmatically validated.
- The final response does not expose a formal citation object mapping each
  claim to source IDs.
- There is no confidence threshold or no-answer threshold based on distance.
- The system gives research assistance, not legal advice or a final legal
  opinion.

## 20. Recommended next engineering steps

1. Remove and rotate the exposed Vertex OAuth token.
2. Move all Vertex settings to environment variables.
3. Add a global exception handler returning useful 4xx/5xx JSON errors.
4. Add a health check for PostgreSQL, Ollama, and Vertex configuration.
5. Add chunk overlap and sentence/paragraph-aware chunking.
6. Add a document content hash and per-chunk embedding status.
7. Process ingestion asynchronously with job status and retries.
8. Batch embedding requests where supported by the model API.
9. Add hybrid lexical plus vector search.
10. Boost Supreme Court authority and apply court/type filters when relevant.
11. Add citation validation and structured claim-to-source output.
12. Add an evaluation set with expected authorities and answer rubrics.
13. Add authentication before exposing ingestion or query APIs outside localhost.
14. Add request timeouts, retry policy, rate limiting, and structured logging.

## 21. End-to-end request trace

For a question such as:

```text
What is reasonable compensation for breach of contract?
```

the code path is:

```text
LegalAnswerController.ask
        |
        v
LegalAnswerService.answer
        |
        +--> LegalSearchService.search
        |          |
        |          +--> OllamaEmbeddingClient.embed
        |          |
        |          +--> LegalSearchRepository.findNearest
        |                       |
        |                       +--> PostgreSQL pgvector <=> search
        |
        +--> LegalAnswerService.buildPrompt
        |
        +--> VertexAiAnswerClient.generate
                   |
                   +--> Vertex AI Gemini generateContent
        |
        v
LegalAnswer(answer, sources)
```

For an initial corpus load, the code path is:

```text
CorpusIngestionController.ingestCorpus
        |
        v
CorpusIngestionService.ingestAll
        |
        +--> walk judgment metadata files
        |       |
        |       +--> ObjectMapper JSON parsing
        |       +--> Document persistence
        |       +--> PdfTextExtractor.extractPages
        |       +--> TextChunker.chunk
        |       +--> LegalChunkRepository.saveAndFlush
        |       +--> OllamaEmbeddingClient.embed
        |       +--> LegalVectorRepository.saveEmbedding
        |
        +--> walk statute PDFs
                |
                +--> derive synthetic statute Document
                +--> same PDF/chunk/embed/vector path
```

This is the current RAG implementation: local semantic indexing and retrieval,
cloud-hosted grounded synthesis, and a frontend that exposes the workflow to a
research user.
