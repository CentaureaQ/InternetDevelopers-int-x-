package com.sspku.agent.module.workflow.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowTraceEvent {
    private String nodeId;
    private String nodeType;
    /** running / success / error */
    private String status;
    private Long startedAt;
    private Long finishedAt;
    private Object input;
    private Object output;
    private String error;
}
