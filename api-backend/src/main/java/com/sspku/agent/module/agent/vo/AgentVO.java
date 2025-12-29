package com.sspku.agent.module.agent.vo;

import com.sspku.agent.module.agent.model.ModelConfig;
import com.sspku.agent.module.knowledge.dto.RagConfig;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能体视图对象
 */
@Data
@Builder
public class AgentVO {
    private Long id;
    private String name;
    private String description;
    private String systemPrompt;
    private String userPromptTemplate;
    private ModelConfig modelConfig;
    private List<Long> pluginIds;
    private Long ownerUserId;
    private Long knowledgeBaseId;
    private RagConfig ragConfig;
    private Long workflowId; // 关联的工作流ID
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
