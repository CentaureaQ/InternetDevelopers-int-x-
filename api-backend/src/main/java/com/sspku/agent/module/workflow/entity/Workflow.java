package com.sspku.agent.module.workflow.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流实体（MVP）
 */
@Data
public class Workflow {
    private Long id;
    private Long ownerUserId;
    private String name;
    private String description;
    /** draft / published */
    private String status;
    /** version string, e.g. "1.0" */
    private String version;
    /** graph JSON string */
    private String graph;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
