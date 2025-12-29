package com.sspku.agent.module.workflow.service;

import com.sspku.agent.common.api.PageResponse;
import com.sspku.agent.module.workflow.dto.WorkflowCreateRequest;
import com.sspku.agent.module.workflow.dto.WorkflowDebugRequest;
import com.sspku.agent.module.workflow.dto.WorkflowListQuery;
import com.sspku.agent.module.workflow.dto.WorkflowUpdateRequest;
import com.sspku.agent.module.workflow.vo.WorkflowDebugResponse;
import com.sspku.agent.module.workflow.vo.WorkflowVO;

public interface WorkflowService {

    Long createWorkflow(WorkflowCreateRequest request);

    void updateWorkflow(Long id, WorkflowUpdateRequest request);

    void deleteWorkflow(Long id);

    WorkflowVO getWorkflow(Long id);

    PageResponse<WorkflowVO> listWorkflows(WorkflowListQuery query);

    void publishWorkflow(Long id);

    void unpublishWorkflow(Long id);

    WorkflowDebugResponse debugWorkflow(Long id, WorkflowDebugRequest request);

    /**
     * 执行工作流（仅允许已发布的工作流）
     * 与 debugWorkflow 的区别：此方法会检查工作流状态，只允许 published 状态的工作流执行
     */
    WorkflowDebugResponse executeWorkflow(Long id, WorkflowDebugRequest request);
}
