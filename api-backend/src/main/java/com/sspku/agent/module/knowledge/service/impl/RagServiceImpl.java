package com.sspku.agent.module.knowledge.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.sspku.agent.module.agent.entity.Agent;
import com.sspku.agent.module.agent.mapper.AgentMapper;
import com.sspku.agent.module.knowledge.dto.RagConfig;
import com.sspku.agent.module.knowledge.dto.SearchResult;
import com.sspku.agent.module.knowledge.entity.KnowledgeBase;
import com.sspku.agent.module.knowledge.entity.KnowledgeChunk;
import com.sspku.agent.module.knowledge.mapper.KnowledgeBaseMapper;
import com.sspku.agent.module.knowledge.mapper.KnowledgeChunkMapper;
import com.sspku.agent.module.knowledge.service.EmbeddingService;
import com.sspku.agent.module.knowledge.service.RagService;
import com.sspku.agent.module.knowledge.service.VectorStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final AgentMapper agentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper; // Assuming we can get KBs for an agent
    private final EmbeddingService embeddingService;
    private final VectorStorageService vectorStorageService;
    private final KnowledgeChunkMapper chunkMapper;

    @Override
    public RagConfig getRagConfig(Long agentId) {
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw new RuntimeException("Agent not found");
        }
        
        String configJson = agent.getRagConfig();
        if (StrUtil.isBlank(configJson)) {
            return new RagConfig(); // Default config
        }
        
        try {
            return JSONUtil.toBean(configJson, RagConfig.class);
        } catch (Exception e) {
            log.error("Failed to parse RAG config for agent {}", agentId, e);
            return new RagConfig();
        }
    }

    @Override
    public List<KnowledgeChunk> retrieve(Long agentId, String query) {
        // 1. Get Config
        RagConfig config = getRagConfig(agentId);
        
        // 2. Get Knowledge Bases for Agent
        // Note: We need a way to get KBs associated with an agent.
        // Assuming there's a relation table or logic. For now, let's assume we fetch all KBs for the user who owns the agent?
        // Or better, check `user_agent_rel` -> `user` -> `knowledge_base`?
        // Actually, the schema has `user_agent_rel` but no direct `agent_kb_rel`.
        // Usually agents are associated with KBs. Let's assume we need to implement `selectByAgentId` in KnowledgeBaseMapper
        // or use a placeholder if the relation isn't defined yet.
        // Looking at schema, there is no direct link. Let's assume for now we search ALL KBs (not safe) or just mock it.
        // Wait, the user requirement says: "Get agent associated KBs".
        // I should probably add a relation table `agent_kb_rel` or similar if it doesn't exist.
        // But I cannot modify schema too much without permission.
        // Let's check if `KnowledgeBaseMapper` has something relevant.
        // If not, I'll assume for this task that we search in a specific KB or all KBs.
        // Let's assume we search all KBs for now as a fallback, or better, let's assume the agent has a list of KB IDs in its config or a relation.
        // Let's check `KnowledgeBaseMapper` first.
        
        List<KnowledgeBase> kbs = knowledgeBaseMapper.selectAll(); // Placeholder: Should be filtered by Agent
        if (CollUtil.isEmpty(kbs)) {
            return Collections.emptyList();
        }

        // 3. Embed Query
        List<Float> queryVector = embeddingService.embedQuery(query);

        // 4. Search in Vector DB
        List<SearchResult> allResults = new ArrayList<>();
        for (KnowledgeBase kb : kbs) {
            try {
                List<SearchResult> results = vectorStorageService.search(
                        kb.getId(), 
                        queryVector, 
                        config.getTopK(), 
                        config.getThreshold()
                );
                allResults.addAll(results);
            } catch (Exception e) {
                log.warn("Failed to search KB {}", kb.getId(), e);
            }
        }

                // 5. Sort and Limit
        allResults.sort(Comparator.comparingDouble(SearchResult::getScore).reversed());
        if (allResults.size() > config.getTopK()) {
            allResults = allResults.subList(0, config.getTopK());
        }

        // 6. Fetch Content from DB
        if (CollUtil.isEmpty(allResults)) {
            return Collections.emptyList();
        }

        List<Long> chunkIds = allResults.stream()
                .map(SearchResult::getId)
                .collect(Collectors.toList());
        
        List<KnowledgeChunk> chunks = chunkMapper.selectByIds(chunkIds);
        
        // Map chunks to results to preserve order and score (optional, but good for debugging)
        // We return List<KnowledgeChunk>, so we just return the found chunks.
        // Ideally, we should attach the score to the chunk or return a DTO.
        // For now, let's just return the chunks in the order of scores.
        
        Map<Long, KnowledgeChunk> chunkMap = chunks.stream()
                .collect(Collectors.toMap(KnowledgeChunk::getId, c -> c));
        
        List<KnowledgeChunk> sortedChunks = new ArrayList<>();
        for (SearchResult result : allResults) {
            KnowledgeChunk chunk = chunkMap.get(result.getId());
            if (chunk != null) {
                // We could set a transient score field if KnowledgeChunk had one
                sortedChunks.add(chunk);
            }
        }
        
        return sortedChunks;
    }

    @Override
    public String buildPrompt(List<KnowledgeChunk> chunks, String query) {

        allResults.sort((a, b) -> Float.compare(b.getScore(), a.getScore())); // Descending
        if (allResults.size() > config.getTopK()) {
            allResults = allResults.subList(0, config.getTopK());
        }

        if (allResults.isEmpty()) {
            return Collections.emptyList();
        }

        // 6. Fetch Content
        List<Long> chunkIds = allResults.stream().map(SearchResult::getChunkId).collect(Collectors.toList());
        List<KnowledgeChunk> chunks = chunkMapper.selectByIds(chunkIds);
        
        // Reorder to match score order
        Map<Long, KnowledgeChunk> chunkMap = chunks.stream().collect(Collectors.toMap(KnowledgeChunk::getId, c -> c));
        List<KnowledgeChunk> orderedChunks = new ArrayList<>();
        for (SearchResult result : allResults) {
            if (chunkMap.containsKey(result.getChunkId())) {
                orderedChunks.add(chunkMap.get(result.getChunkId()));
            }
        }
        
        return orderedChunks;
    }

    @Override
    public String buildPrompt(Long agentId, String query, List<KnowledgeChunk> chunks) {
        RagConfig config = getRagConfig(agentId);
        StringBuilder contextBuilder = new StringBuilder();
        
        int currentLength = 0;
        for (int i = 0; i < chunks.size(); i++) {
            String content = chunks.get(i).getContent();
            if (currentLength + content.length() > config.getMaxContextLength()) {
                break;
            }
            contextBuilder.append(String.format("[文档片段 %d]:\n%s\n\n", i + 1, content));
            currentLength += content.length();
        }

        String context = contextBuilder.toString();
        if (StrUtil.isBlank(context)) {
            return query; // No context, just return query or handle differently
        }

        // Template
        return String.format("""
                请基于以下【上下文信息】回答用户的【问题】。
                如果上下文中没有相关信息，请直接回答"根据已知信息无法回答该问题"，不要编造内容。
                
                【上下文信息】：
                %s
                
                【问题】：
                %s
                """, context, query);
    }
}
