package com.sspku.agent.module.knowledge.service;

import java.util.List;

public interface EmbeddingService {
    /**
     * Generate embeddings for a list of texts.
     * @param texts List of text chunks
     * @return List of embedding vectors (List of Float arrays)
     */
    List<List<Float>> embedDocuments(List<String> texts);

    /**
     * Generate embedding for a single query.
     * @param text Query text
     * @return Embedding vector
     */
    List<Float> embedQuery(String text);
}
