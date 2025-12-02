package com.sspku.agent.module.agent.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 更新智能体请求
 */
@Data
public class AgentUpdateRequest {

    @Size(max = 100, message = "名称长度不能超过100字符")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500字符")
    private String description;

    private String systemPrompt;

    private String userPromptTemplate;

    @Valid
    private ModelConfigRequest modelConfig;

    private List<Long> pluginIds;
}
