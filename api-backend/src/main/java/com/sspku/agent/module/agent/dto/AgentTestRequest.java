package com.sspku.agent.module.agent.dto;

import com.sspku.agent.module.knowledge.dto.RagConfig;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 智能体调试请求
 */
@Data
public class AgentTestRequest {

    @NotBlank(message = "测试问题不能为空")
    private String question;

    private RagConfig ragConfig;

    /**
     * 前端会话历史（按时间顺序），用于推理上下文；不做持久化
     */
    private List<ConversationMessage> messages;
}
