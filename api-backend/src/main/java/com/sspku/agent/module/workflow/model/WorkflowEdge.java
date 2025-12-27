package com.sspku.agent.module.workflow.model;

import lombok.Data;

@Data
public class WorkflowEdge {
    private String from;
    private String to;
    private String label;
}
