package com.sspku.agent.module.agent.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能体实体
 */
@Data
public class Agent {
    private Long id;
    private String name;
    private String description;
    private String systemPrompt;
    private String userPromptTemplate;
    private String modelConfig;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
