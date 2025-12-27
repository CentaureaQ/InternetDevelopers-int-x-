package com.sspku.agent.module.agent.service;

import com.sspku.agent.common.api.PageResponse;
import com.sspku.agent.module.agent.dto.AgentCreateRequest;
import com.sspku.agent.module.agent.dto.AgentListQuery;
import com.sspku.agent.module.agent.dto.AgentTestRequest;
import com.sspku.agent.module.agent.dto.AgentUpdateRequest;
import com.sspku.agent.module.agent.vo.AgentTestResponse;
import com.sspku.agent.module.agent.vo.AgentVO;

/**
 * 智能体服务
 */
public interface AgentService {

    Long createAgent(AgentCreateRequest request);

    void updateAgent(Long id, AgentUpdateRequest request);

    AgentVO getAgent(Long id);

    PageResponse<AgentVO> listAgents(AgentListQuery query);

    void publishAgent(Long id);

    void unpublishAgent(Long id);

    void deleteAgent(Long id);

    AgentTestResponse testAgent(Long id, AgentTestRequest request);
}
