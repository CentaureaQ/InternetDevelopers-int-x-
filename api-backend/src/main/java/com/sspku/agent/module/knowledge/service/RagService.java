package com.sspku.agent.module.knowledge.service;

import com.sspku.agent.module.knowledge.dto.RagConfig;
import com.sspku.agent.module.knowledge.entity.KnowledgeChunk;

import java.util.List;

public interface RagService {
    /**
     * Retrieve relevant chunks for a query based on agent's configuration.
     * @param agentId Agent ID
     * @param query User query
     * @return List of relevant knowledge chunks
     */
    List<KnowledgeChunk> retrieve(Long agentId, String query);

    /**
     * Retrieve relevant chunks for a query with specific configuration.
     * @param agentId Agent ID
     * @param query User query
     * @param config RAG configuration
     * @return List of relevant knowledge chunks
     */
    List<KnowledgeChunk> retrieve(Long agentId, String query, RagConfig config);

    /**
     * Build the final prompt with context.
     * @param agentId Agent ID
     * @param query User query
     * @param chunks Retrieved chunks
     * @return Prompt string
     */
    String buildPrompt(Long agentId, String query, List<KnowledgeChunk> chunks);

    /**
     * Build the final prompt with context and specific configuration.
     * @param agentId Agent ID
     * @param query User query
     * @param chunks Retrieved chunks
     * @param config RAG configuration
     * @return Prompt string
     */
    String buildPrompt(Long agentId, String query, List<KnowledgeChunk> chunks, RagConfig config);
    
    /**
     * Get RAG configuration for an agent.
     * @param agentId Agent ID
     * @return RagConfig
     */
    RagConfig getRagConfig(Long agentId);
}
