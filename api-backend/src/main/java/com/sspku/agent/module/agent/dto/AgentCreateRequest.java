package com.sspku.agent.module.agent.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建智能体请求
 */
@Data
public class AgentCreateRequest {

    @NotBlank(message = "名称不能为空")
    @Size(max = 100, message = "名称长度不能超过100字符")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500字符")
    private String description;

    @NotBlank(message = "系统提示词不能为空")
    private String systemPrompt;

    private String userPromptTemplate;

    @NotNull(message = "请配置模型参数")
    @Valid
    private ModelConfigRequest modelConfig;

    private List<Long> pluginIds;

    /**
     * 可选：显式指定智能体归属的用户ID；若不传则自动读取当前登录用户
     */
    private Long ownerUserId;
}
