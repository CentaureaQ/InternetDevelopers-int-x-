package com.sspku.agent.module.agent.controller;

import com.sspku.agent.common.api.ApiResponse;
import com.sspku.agent.common.api.PageResponse;
import com.sspku.agent.module.agent.dto.AgentCreateRequest;
import com.sspku.agent.module.agent.dto.AgentListQuery;
import com.sspku.agent.module.agent.dto.AgentTestRequest;
import com.sspku.agent.module.agent.dto.AgentUpdateRequest;
import com.sspku.agent.module.agent.service.AgentService;
import com.sspku.agent.module.agent.vo.AgentTestResponse;
import com.sspku.agent.module.agent.vo.AgentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 智能体控制器
 */
@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping
    public ApiResponse<Long> createAgent(@Valid @RequestBody AgentCreateRequest request) {
        Long id = agentService.createAgent(request);
        return ApiResponse.ok("智能体创建成功", id);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateAgent(@PathVariable Long id, @Valid @RequestBody AgentUpdateRequest request) {
        agentService.updateAgent(id, request);
        return ApiResponse.ok("智能体更新成功", null);
    }

    @GetMapping("/{id}")
    public ApiResponse<AgentVO> getAgent(@PathVariable Long id) {
        return ApiResponse.ok(agentService.getAgent(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<AgentVO>> listAgents(@Valid AgentListQuery query) {
        return ApiResponse.ok(agentService.listAgents(query));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<Void> publish(@PathVariable Long id) {
        agentService.publishAgent(id);
        return ApiResponse.ok("智能体发布成功", null);
    }

    @PostMapping("/{id}/unpublish")
    public ApiResponse<Void> unpublish(@PathVariable Long id) {
        agentService.unpublishAgent(id);
        return ApiResponse.ok("智能体取消发布成功", null);
    }

    @PostMapping("/{id}/test")
    public ApiResponse<AgentTestResponse> test(@PathVariable Long id, @Valid @RequestBody AgentTestRequest request) {
        return ApiResponse.ok(agentService.testAgent(id, request));
    }
}
