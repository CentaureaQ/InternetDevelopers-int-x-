package com.sspku.agent.module.agent.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能体与插件的关联关系
 */
@Data
public class AgentPluginRelation {
    private Long id;
    private Long agentId;
    private Long pluginId;
    private Integer sortOrder;
    private String config;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
