package com.sspku.agent.module.agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 智能体调试请求
 */
@Data
public class AgentTestRequest {

    @NotBlank(message = "测试问题不能为空")
    private String question;
}

