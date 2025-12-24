package com.sspku.agent.module.workflow.controller;

import com.sspku.agent.common.api.ApiResponse;
import com.sspku.agent.common.api.PageResponse;
import com.sspku.agent.module.workflow.dto.WorkflowCreateRequest;
import com.sspku.agent.module.workflow.dto.WorkflowDebugRequest;
import com.sspku.agent.module.workflow.dto.WorkflowListQuery;
import com.sspku.agent.module.workflow.dto.WorkflowUpdateRequest;
import com.sspku.agent.module.workflow.service.WorkflowService;
import com.sspku.agent.module.workflow.vo.WorkflowDebugResponse;
import com.sspku.agent.module.workflow.vo.WorkflowVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody WorkflowCreateRequest request) {
        Long id = workflowService.createWorkflow(request);
        return ApiResponse.ok("工作流创建成功", id);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody WorkflowUpdateRequest request) {
        workflowService.updateWorkflow(id, request);
        return ApiResponse.ok("工作流更新成功", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        workflowService.deleteWorkflow(id);
        return ApiResponse.ok("工作流删除成功", null);
    }

    @GetMapping("/{id}")
    public ApiResponse<WorkflowVO> get(@PathVariable Long id) {
        return ApiResponse.ok(workflowService.getWorkflow(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<WorkflowVO>> list(@Valid WorkflowListQuery query) {
        return ApiResponse.ok(workflowService.listWorkflows(query));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<Void> publish(@PathVariable Long id) {
        workflowService.publishWorkflow(id);
        return ApiResponse.ok("工作流发布成功", null);
    }

    @PostMapping("/{id}/unpublish")
    public ApiResponse<Void> unpublish(@PathVariable Long id) {
        workflowService.unpublishWorkflow(id);
        return ApiResponse.ok("工作流取消发布成功", null);
    }

    @PostMapping("/{id}/debug")
    public ApiResponse<WorkflowDebugResponse> debug(@PathVariable Long id, @RequestBody WorkflowDebugRequest request) {
        return ApiResponse.ok(workflowService.debugWorkflow(id, request));
    }
}
