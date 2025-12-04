package com.sspku.agent.module.knowledge.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KnowledgeChunk {
    private Long id;
    private Long documentId;
    private Long kbId;
    private Integer chunkIndex;
    private String content;
    private Integer tokenCount;
    private LocalDateTime createdAt;
}
