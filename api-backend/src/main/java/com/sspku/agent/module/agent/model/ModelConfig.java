package com.sspku.agent.module.agent.model;

import lombok.Data;

/**
 * 智能体模型配置
 */
@Data
public class ModelConfig {
    private String provider;
    private String model;
    private Double temperature;
    private Integer maxTokens;
}

