package com.sspku.agent.module.agent.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户与智能体的关系
 */
@Data
public class UserAgentRelation {
    private Long id;
    private Long userId;
    private Long agentId;
    private String relationType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
