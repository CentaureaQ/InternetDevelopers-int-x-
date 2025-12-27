package com.sspku.agent.module.knowledge.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KnowledgeDocument {
    private Long id;
    private String uuid;
    private String name;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private Long knowledgeBaseId;
    private String status; // uploading/processing/processed/failed
    private Integer chunkCount;
    private String errorMessage;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
