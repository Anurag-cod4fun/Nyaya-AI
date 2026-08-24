# Nyaya AI Backend

Spring Boot 4.1.0 backend using Java 25, PostgreSQL/pgvector, JPA, Flyway,
PDFBox, validation, Actuator, and Ollama.

## Run

```powershell
mvn spring-boot:run
```

Configure PostgreSQL with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and optionally `JPA_DDL_AUTO`.

## RAG endpoints

- `POST /api/ingestion/corpus` ingests judgment metadata/PDFs and statute PDFs from `CORPUS_ROOT`.
- `POST /api/search` embeds a query and returns nearest legal chunks.
- `POST /api/ask` retrieves chunks and generates an answer with source markers.

The embedding model is `bge-m3` in Ollama. Answer generation uses Vertex AI
Gemini through `VERTEX_AI_URL` and `VERTEX_AI_ACCESS_TOKEN`; the local Llama
model is not required. OAuth access tokens are temporary and must be refreshed
when they expire.
