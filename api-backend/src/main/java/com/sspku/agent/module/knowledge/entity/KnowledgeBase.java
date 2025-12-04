package com.sspku.agent.module.knowledge.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KnowledgeBase {
    private Long id;
    private String uuid;
    private String name;
    private String description;
    private String icon;
    private String level; // system/school/course/agent/personal
    private Long parentKbId;
    private String vectorDbType;
    private String embeddingModelId;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private String retrievalConfig; // JSON string
    private String accessLevel; // public/protected/private
    private Long ownerId;
    private Integer documentCount;
    private Integer chunkCount;
    private Long totalSize;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
