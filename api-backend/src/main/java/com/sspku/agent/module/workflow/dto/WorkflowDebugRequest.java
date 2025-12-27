package com.sspku.agent.module.workflow.dto;

import lombok.Data;

import java.util.Map;

@Data
public class WorkflowDebugRequest {
    private Map<String, Object> inputs;
}
