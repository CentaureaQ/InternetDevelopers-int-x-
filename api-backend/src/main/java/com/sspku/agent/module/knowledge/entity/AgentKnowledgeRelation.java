package com.sspku.agent.module.knowledge.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 智能体知识库关联实体
 */
@Data
public class AgentKnowledgeRelation {
    private Long id;
    private Long agentId;
    private Long knowledgeBaseId;
    private LocalDateTime createdAt;
}