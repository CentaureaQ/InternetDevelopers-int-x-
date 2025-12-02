package com.sspku.agent.module.agent.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sspku.agent.common.api.PageResponse;
import com.sspku.agent.common.exception.BusinessException;
import com.sspku.agent.module.agent.dto.AgentCreateRequest;
import com.sspku.agent.module.agent.dto.AgentListQuery;
import com.sspku.agent.module.agent.dto.AgentTestRequest;
import com.sspku.agent.module.agent.dto.AgentUpdateRequest;
import com.sspku.agent.module.agent.dto.ModelConfigRequest;
import com.sspku.agent.module.agent.entity.Agent;
import com.sspku.agent.module.agent.mapper.AgentMapper;
import com.sspku.agent.module.agent.mapper.AgentPluginRelationMapper;
import com.sspku.agent.module.agent.mapper.UserAgentRelationMapper;
import com.sspku.agent.module.agent.mapper.UserPluginRelationMapper;
import com.sspku.agent.module.agent.entity.AgentPluginRelation;
import com.sspku.agent.module.agent.entity.UserAgentRelation;
import com.sspku.agent.module.agent.model.ModelConfig;
import com.sspku.agent.module.agent.service.AgentService;
import com.sspku.agent.module.agent.vo.AgentTestResponse;
import com.sspku.agent.module.agent.vo.AgentVO;
import com.sspku.agent.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 智能体服务实现
 */
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final AgentMapper agentMapper;
    private final AgentPluginRelationMapper agentPluginRelationMapper;
    private final UserAgentRelationMapper userAgentRelationMapper;
    private final UserPluginRelationMapper userPluginRelationMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAgent(AgentCreateRequest request) {
        validateModelConfig(request.getModelConfig());
        Long ownerUserId = resolveOwnerId(request.getOwnerUserId());
        Agent agent = new Agent();
        agent.setName(request.getName());
        agent.setDescription(request.getDescription());
        agent.setSystemPrompt(request.getSystemPrompt());
        agent.setUserPromptTemplate(request.getUserPromptTemplate());
        agent.setModelConfig(writeModelConfig(request.getModelConfig()));
        agent.setStatus("draft");

        agentMapper.insert(agent);
        userAgentRelationMapper.upsertOwner(ownerUserId, agent.getId());
        saveAgentPlugins(agent.getId(), request.getPluginIds());
        syncUserPlugins(ownerUserId, request.getPluginIds());
        return agent.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAgent(Long id, AgentUpdateRequest request) {
        Agent agent = agentMapper.selectById(id);
        if (agent == null) {
            throw new BusinessException("智能体不存在");
        }

        if (request.getName() != null) {
            agent.setName(request.getName());
        }
        if (request.getDescription() != null) {
            agent.setDescription(request.getDescription());
        }
        if (request.getSystemPrompt() != null) {
            agent.setSystemPrompt(request.getSystemPrompt());
        }
        if (request.getUserPromptTemplate() != null) {
            agent.setUserPromptTemplate(request.getUserPromptTemplate());
        }
        if (request.getModelConfig() != null) {
            validateModelConfig(request.getModelConfig());
            agent.setModelConfig(writeModelConfig(request.getModelConfig()));
        }

        agentMapper.update(agent);

        if (request.getPluginIds() != null) {
            saveAgentPlugins(id, request.getPluginIds());
            Long ownerUserId = userAgentRelationMapper.selectOwnerId(id);
            syncUserPlugins(ownerUserId, request.getPluginIds());
        }
    }

    @Override
    public AgentVO getAgent(Long id) {
        Agent agent = agentMapper.selectById(id);
        if (agent == null) {
            throw new BusinessException("智能体不存在");
        }
        List<Long> pluginIds = agentPluginRelationMapper.selectPluginIdsByAgentId(id);
        Long ownerId = userAgentRelationMapper.selectOwnerId(id);
        return convertToVO(agent, pluginIds, ownerId);
    }

    @Override
    public PageResponse<AgentVO> listAgents(AgentListQuery query) {
        int pageNo = query.getPageNo() != null && query.getPageNo() > 0 ? query.getPageNo() : 1;
        int pageSize = query.getPageSize() != null && query.getPageSize() > 0 ? query.getPageSize() : 20;
        int offset = (pageNo - 1) * pageSize;

        long total = agentMapper.countByCondition(query.getKeyword(), query.getStatus());
        if (total == 0) {
            return PageResponse.empty(pageNo, pageSize);
        }

        List<Agent> agents = agentMapper.selectPageByCondition(query.getKeyword(), query.getStatus(), pageSize, offset);
        List<Long> agentIds = agents.stream().map(Agent::getId).collect(Collectors.toList());

        final Map<Long, List<Long>> pluginMap;
        if (!agentIds.isEmpty()) {
            List<AgentPluginRelation> relations = agentPluginRelationMapper.selectByAgentIds(agentIds);
            pluginMap = relations.stream().collect(
                    Collectors.groupingBy(AgentPluginRelation::getAgentId,
                            Collectors.mapping(AgentPluginRelation::getPluginId, Collectors.toList())));
        } else {
            pluginMap = Collections.emptyMap();
        }

        final Map<Long, Long> ownerMap;
        if (!agentIds.isEmpty()) {
            List<UserAgentRelation> owners = userAgentRelationMapper.selectByAgentIds(agentIds);
            ownerMap = owners.stream().collect(
                    Collectors.toMap(UserAgentRelation::getAgentId, UserAgentRelation::getUserId,
                            (existing, replacement) -> existing));
        } else {
            ownerMap = Collections.emptyMap();
        }

        List<AgentVO> vos = agents.stream()
                .map(agent -> convertToVO(agent,
                        pluginMap.getOrDefault(agent.getId(), Collections.emptyList()),
                        ownerMap.get(agent.getId())))
                .collect(Collectors.toList());
        return PageResponse.of(total, pageNo, pageSize, vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishAgent(Long id) {
        Agent agent = agentMapper.selectById(id);
        if (agent == null) {
            throw new BusinessException("智能体不存在");
        }
        if (!StringUtils.hasText(agent.getName())) {
            throw new BusinessException("请填写智能体名称");
        }
        if (!StringUtils.hasText(agent.getSystemPrompt())) {
            throw new BusinessException("请配置系统提示词");
        }
        if (!StringUtils.hasText(agent.getModelConfig())) {
            throw new BusinessException("请配置模型参数");
        }

        agent.setStatus("published");
        agentMapper.updateStatus(agent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpublishAgent(Long id) {
        Agent agent = agentMapper.selectById(id);
        if (agent == null) {
            throw new BusinessException("智能体不存在");
        }

        agent.setStatus("draft");
        agentMapper.updateStatus(agent);
    }

    @Override
    public AgentTestResponse testAgent(Long id, AgentTestRequest request) {
        Agent agent = agentMapper.selectById(id);
        if (agent == null) {
            throw new BusinessException("智能体不存在");
        }
        long start = Instant.now().toEpochMilli();
        String reply = "[Mock Reply] " + request.getQuestion();
        long elapsed = Instant.now().toEpochMilli() - start;
        return AgentTestResponse.builder()
                .reply(reply)
                .elapsedMs(elapsed)
                .promptTokens(request.getQuestion().length() / 4 + 1)
                .completionTokens(reply.length() / 4 + 1)
                .build();
    }

    private void validateModelConfig(ModelConfigRequest modelConfig) {
        if (modelConfig == null) {
            throw new BusinessException("模型配置不能为空");
        }
        if (!StringUtils.hasText(modelConfig.getProvider())) {
            throw new BusinessException("模型服务商不能为空");
        }
        if (!StringUtils.hasText(modelConfig.getModel())) {
            throw new BusinessException("模型名称不能为空");
        }
    }

    private String writeModelConfig(ModelConfigRequest modelConfigRequest) {
        ModelConfig config = new ModelConfig();
        config.setProvider(modelConfigRequest.getProvider());
        config.setModel(modelConfigRequest.getModel());
        config.setTemperature(modelConfigRequest.getTemperature());
        config.setMaxTokens(modelConfigRequest.getMaxTokens());
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new BusinessException("模型配置序列化失败");
        }
    }

    private ModelConfig readModelConfig(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ModelConfig.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private AgentVO convertToVO(Agent agent, List<Long> pluginIds, Long ownerUserId) {
        return AgentVO.builder()
                .id(agent.getId())
                .name(agent.getName())
                .description(agent.getDescription())
                .systemPrompt(agent.getSystemPrompt())
                .userPromptTemplate(agent.getUserPromptTemplate())
                .modelConfig(readModelConfig(agent.getModelConfig()))
                .pluginIds(pluginIds)
                .ownerUserId(ownerUserId)
                .status(agent.getStatus())
                .createdAt(agent.getCreatedAt())
                .updatedAt(agent.getUpdatedAt())
                .build();
    }

    private void saveAgentPlugins(Long agentId, List<Long> pluginIds) {
        agentPluginRelationMapper.deleteByAgentId(agentId);
        if (!CollectionUtils.isEmpty(pluginIds)) {
            agentPluginRelationMapper.insertBatch(agentId, pluginIds);
        }
    }

    private void syncUserPlugins(Long userId, List<Long> pluginIds) {
        if (userId == null || CollectionUtils.isEmpty(pluginIds)) {
            return;
        }
        userPluginRelationMapper.insertOrUpdate(userId, pluginIds);
    }

    private Long resolveOwnerId(Long ownerUserId) {
        if (ownerUserId != null) {
            return ownerUserId;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        throw new BusinessException("无法确定当前用户，请登录或显式传入 ownerUserId");
    }
}
