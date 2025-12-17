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
        return retrieve(agentId, query, getRagConfig(agentId));
    }

    @Override
    public List<KnowledgeChunk> retrieve(Long agentId, String query, RagConfig config) {
        log.info("RAG Retrieve started for agent {} with query: {}", agentId, query);
        if (config == null) {
            config = getRagConfig(agentId);
        }
        log.info("RAG Config: {}", JSONUtil.toJsonStr(config));
        
        // 2. Get Knowledge Bases for Agent
        List<KnowledgeBase> kbs = knowledgeBaseMapper.selectByAgentId(agentId);
        if (CollUtil.isEmpty(kbs)) {
            log.warn("No Knowledge Bases found for agent {}", agentId);
            return Collections.emptyList();
        }
        log.info("Found {} Knowledge Bases", kbs.size());

        // 3. Embed Query
        List<Float> queryVector = embeddingService.embedQuery(query);
        log.info("Query embedded, vector size: {}", queryVector.size());

        // 4. Search in Vector DB
        List<SearchResult> allResults = new ArrayList<>();
        for (KnowledgeBase kb : kbs) {
            try {
                log.info("Searching in KB: {}", kb.getId());
                List<SearchResult> results = vectorStorageService.search(
                        kb.getId(), 
                        queryVector, 
                        config.getTopK(), 
                        config.getThreshold()
                );
                log.info("KB {} returned {} results", kb.getId(), results.size());
                allResults.addAll(results);
            } catch (Exception e) {
                log.warn("Failed to search KB {}", kb.getId(), e);
            }
        }

        // 5. Sort and Limit
        allResults.sort(Comparator.comparingDouble(SearchResult::getScore).reversed());
        log.info("Total results found: {}", allResults.size());
        
        if (allResults.size() > config.getTopK()) {
            allResults = allResults.subList(0, config.getTopK());
        }

        // 6. Fetch Content from DB
        if (CollUtil.isEmpty(allResults)) {
            log.info("No results after filtering");
            return Collections.emptyList();
        }

        List<Long> chunkIds = allResults.stream()
                .map(SearchResult::getChunkId)
                .collect(Collectors.toList());
        
        List<KnowledgeChunk> chunks = chunkMapper.selectByIds(chunkIds);
        log.info("Retrieved {} chunks from DB", chunks.size());
        
        // Map chunks to results to preserve order and score
        Map<Long, KnowledgeChunk> chunkMap = chunks.stream()
                .collect(Collectors.toMap(KnowledgeChunk::getId, c -> c));
        
        List<KnowledgeChunk> sortedChunks = new ArrayList<>();
        for (SearchResult result : allResults) {
            KnowledgeChunk chunk = chunkMap.get(result.getChunkId());
            if (chunk != null) {
                sortedChunks.add(chunk);
            }
        }
        
        return sortedChunks;
    }

    @Override
    public String buildPrompt(Long agentId, String query, List<KnowledgeChunk> chunks) {
        return buildPrompt(agentId, query, chunks, getRagConfig(agentId));
    }

    @Override
    public String buildPrompt(Long agentId, String query, List<KnowledgeChunk> chunks, RagConfig config) {
        if (config == null) {
            config = getRagConfig(agentId);
        }
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
