package com.sspku.agent.module.workflow.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WorkflowVO {
    private Long id;
    private String name;
    private String description;
    /** draft / published */
    private String status;
    private String version;
    private String graph;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
