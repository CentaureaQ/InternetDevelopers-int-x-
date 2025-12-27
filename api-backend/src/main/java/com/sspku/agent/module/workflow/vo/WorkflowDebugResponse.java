package com.sspku.agent.module.workflow.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WorkflowDebugResponse {
    private Object output;
    private List<WorkflowTraceEvent> trace;
}
