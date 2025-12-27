package com.sspku.agent.module.workflow.model;

import lombok.Data;

import java.util.List;

/**
 * MVP graph schema aligned with web-vue workflow editor.
 */
@Data
public class WorkflowGraph {
    private String version;
    private List<WorkflowNode> nodes;
    private List<WorkflowEdge> edges;
}
