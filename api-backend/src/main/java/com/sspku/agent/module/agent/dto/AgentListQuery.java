package com.sspku.agent.module.agent.dto;

import lombok.Data;

/**
 * 智能体查询条件
 */
@Data
public class AgentListQuery {
    private String keyword;
    private String status;
    private Integer pageNo = 1;
    private Integer pageSize = 20;
}

