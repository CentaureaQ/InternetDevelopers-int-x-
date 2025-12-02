package com.sspku.agent.module.agent.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 智能体调试结果
 */
@Data
@Builder
public class AgentTestResponse {
    private String reply;
    private long elapsedMs;
    private int promptTokens;
    private int completionTokens;
}

