package com.sspku.agent.module.agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 简单对话消息（仅用于调试接口透传会话，不做持久化）
 */
@Data
public class ConversationMessage {

    /**
     * 角色：user 或 assistant
     */
    @NotBlank
    private String role;

    /**
     * 消息内容
     */
    @NotBlank
    private String content;

    /**
     * 可选时间戳（毫秒）
     */
    private Long timestamp;
}
