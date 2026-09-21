---
title: Embedding & RAG
description: Embedding models, embedding stores, and content retrievers for Retrieval-Augmented Generation.
layout: page
---

# Embedding & RAG

The feature pack provides Galleon layers for embedding models, embedding stores, and content retrievers that together enable Retrieval-Augmented Generation (RAG) pipelines in your WildFly applications.

## Embedding Models

In-memory embedding models run locally without external API calls. They are bundled with the feature pack via ONNX runtime.

| Layer | Model | Quantized |
|-------|-------|-----------|
| `in-memory-embedding-model-all-minilm-l6-v2` | all-MiniLM-L6-v2 | No |
| `in-memory-embedding-model-all-minilm-l6-v2-q` | all-MiniLM-L6-v2 | Yes |
| `in-memory-embedding-model-bge-small-en` | BGE-small-en | No |
| `in-memory-embedding-model-bge-small-en-q` | BGE-small-en | Yes |
| `in-memory-embedding-model-bge-small-en-v15` | BGE-small-en-v1.5 | No |
| `in-memory-embedding-model-bge-small-en-v15-q` | BGE-small-en-v1.5 | Yes |
| `in-memory-embedding-model-e5-small-v2` | E5-small-v2 | No |
| `in-memory-embedding-model-e5-small-v2-q` | E5-small-v2 | Yes |

The Ollama layer also supports embeddings:

| Layer | Provider |
|-------|----------|
| `ollama-embedding-model` | Ollama |

### Ollama Embedding Model

```bash
export OLLAMA_EMBEDDING_URL=http://localhost:11434
export OLLAMA_EMBEDDING_MODEL_NAME=nomic-embed-text
export OLLAMA_EMBEDDING_LOG_REQUEST=true
export OLLAMA_EMBEDDING_LOG_RESPONSE=true
```

## Embedding Stores

Embedding stores persist vectorized content for similarity search.

### In-Memory

The `in-memory-embedding-store` layer provides a simple in-memory store suitable for development and small datasets.

```bash
export IN_MEMORY_EMBEDDING_FILE=embeddings.json
```

The `IN_MEMORY_EMBEDDING_FILE` path is relative to the server configuration directory.

### Neo4j

```bash
export NEO4J_URL=bolt://localhost:7687
export NEO4J_USER=neo4j
export NEO4J_PASSWORD=password
export NEO4J_DIMENSION=384
```

### Weaviate

```bash
export WEAVIATE_HOST=localhost
export WEAVIATE_PORT=8080
export WEAVIATE_OBJECT_CLASS=Document
export WEAVIATE_SSL_ENABLED=false
export WEAVIATE_SOCKET_BINDING=weaviate
```

### Chroma

```bash
export CHROMA_URL=http://localhost:8000
export CHROMA_API_VERSION=v1
export CHROMA_LOG_REQUEST=true
export CHROMA_LOG_RESPONSE=true
export CHROMA_TIMEOUT=30000
```

## Content Retrievers

Content retrievers combine an embedding model with an embedding store to find relevant content for a query.

### Default Content Retriever

The `default-embedding-content-retriever` layer provides a ready-to-use retriever using `in-memory-embedding-store` and `in-memory-embedding-model-all-minilm-l6-v2`:

```bash
export EMBEDDING_RETRIEVER_MAX_RESULTS=5
export EMBEDDING_RETRIEVER_MIN_SCORE=0.5
```

### Neo4j Content Retrievers

Specialized retrievers that query a Neo4j graph database using a chat language model to generate Cypher queries:

| Layer | LLM Provider |
|-------|-------------|
| `neo4j-content-retriever` | None (embedding only) |
| `ollama-neo4j-content-retriever` | Ollama |
| `openai-neo4j-content-retriever` | OpenAI |

```bash
export NEO4J_URL=bolt://localhost:7687
export NEO4J_USER=neo4j
export NEO4J_PASSWORD=password
export NEO4J_PROMPT_TEMPLATE="Generate a Cypher query for: \{question}"
export NEO4J_CHAT_LANGUAGE_MODEL=ollama-chat-model
```

## Example: RAG Pipeline

Provision a server with RAG capabilities:

```xml
<layers>
    <layer>cloud-server</layer>
    <layer>ollama-chat-model</layer>
    <layer>default-embedding-content-retriever</layer>
</layers>
```

This gives you:
- An Ollama-backed chat model
- An in-memory embedding model (all-MiniLM-L6-v2)
- An in-memory embedding store
- A content retriever wired to both

## Next Steps

- [Chat Models](chat-models) -- Configure LLM providers
- [Configuration](configuration) -- Full environment variable reference
