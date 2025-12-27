package com.sspku.agent.module.knowledge.dto;

import lombok.Data;

@Data
public class SearchResult {
    private Long chunkId;
    private Long docId;
    private String content;
    private float score;
}
