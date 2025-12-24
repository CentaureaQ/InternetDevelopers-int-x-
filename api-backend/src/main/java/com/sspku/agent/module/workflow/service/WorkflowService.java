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
}
