package com.sspku.agent.module.workflow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class WorkflowListQuery {

    @Min(value = 1, message = "pageNo 必须 >= 1")
    private Integer pageNo;

    @Min(value = 1, message = "pageSize 必须 >= 1")
    @Max(value = 100, message = "pageSize 不能超过100")
    private Integer pageSize;

    private String keyword;

    /** draft / published */
    private String status;
}
