package com.sspku.agent.module.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WorkflowCreateRequest {

    @NotBlank(message = "工作流名称不能为空")
    @Size(max = 100, message = "工作流名称不能超过100字符")
    private String name;

    @Size(max = 500, message = "工作流描述不能超过500字符")
    private String description;

    @NotBlank(message = "graph 不能为空")
    private String graph;
}
