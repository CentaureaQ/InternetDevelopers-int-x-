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
import com.sspku.agent.module.knowledge.entity.KnowledgeChunk;
import com.sspku.agent.module.knowledge.service.RagService;
import com.sspku.agent.module.user.entity.User;
import com.sspku.agent.module.agent.tool.PluginToolFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.*;
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
    private final PluginToolFactory pluginToolFactory;
    private final RagService ragService;

    // Spring AI 聊天模型（使用自动配置的默认模型）
    private final ChatModel chatModel;

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
    @Transactional(rollbackFor = Exception.class)
    public void deleteAgent(Long id) {
        Agent agent = agentMapper.selectById(id);
        if (agent == null) {
            throw new BusinessException("智能体不存在");
        }

        // 删除关联的插件关系
        agentPluginRelationMapper.deleteByAgentId(id);
        // 删除用户关联关系
        userAgentRelationMapper.deleteByAgentId(id);
        // 删除智能体
        agentMapper.deleteById(id);
    }

    @Override
    public AgentTestResponse testAgent(Long id, AgentTestRequest request) {
        Agent agent = agentMapper.selectById(id);
        if (agent == null) {
            throw new BusinessException("智能体不存在");
        }

        // 解析模型配置
        ModelConfig modelConfig = readModelConfig(agent.getModelConfig());
        if (modelConfig == null || !StringUtils.hasText(modelConfig.getModel())) {
            throw new BusinessException("智能体模型配置不完整");
        }

        long start = Instant.now().toEpochMilli();

        try {
            // 构建对话消息
            List<Message> messages = new ArrayList<>();

            // 添加系统提示词
            if (StringUtils.hasText(agent.getSystemPrompt())) {
                messages.add(new SystemMessage(agent.getSystemPrompt()));
            }

            // RAG: 检索相关知识
            String userQuestion = request.getQuestion();
            try {
                List<KnowledgeChunk> chunks = ragService.retrieve(id, userQuestion);
                if (!CollectionUtils.isEmpty(chunks)) {
                    userQuestion = ragService.buildPrompt(id, userQuestion, chunks);
                }
            } catch (Exception e) {
                // RAG 失败不影响主流程
                e.printStackTrace();
            }

            // 添加用户问题
            messages.add(new UserMessage(userQuestion));

            // 获取绑定的插件并转换为 ToolCallback
            List<Long> pluginIds = agentPluginRelationMapper.selectPluginIdsByAgentId(id);
            List<ToolCallback> toolCallbacks = pluginToolFactory.createToolCallbacks(pluginIds);

            // 使用智能体配置的参数创建运行时 ChatOptions
            var optionsBuilder = org.springframework.ai.model.tool.ToolCallingChatOptions
                    .builder()
                    .model(modelConfig.getModel())
                    .temperature(modelConfig.getTemperature() != null ? modelConfig.getTemperature() : 0.7)
                    .topP(modelConfig.getTopP() != null ? modelConfig.getTopP() : 0.9);

            if (!CollectionUtils.isEmpty(toolCallbacks)) {
                optionsBuilder.toolCallbacks(toolCallbacks);
            }

            var options = optionsBuilder.build();

            // 创建带有选项的 Prompt
            Prompt prompt = new Prompt(messages, options);

            // 调用模型
            ChatResponse response = chatModel.call(prompt);

            // 提取回复内容
            String reply = response.getResult().getOutput().getText();

            long elapsed = Instant.now().toEpochMilli() - start;

            // 提取 token 使用情况
            Integer promptTokens = null;
            Integer completionTokens = null;
            if (response.getMetadata().getUsage() != null) {
                promptTokens = response.getMetadata().getUsage().getPromptTokens();
                completionTokens = response.getMetadata().getUsage().getCompletionTokens();
            }

            return AgentTestResponse.builder()
                    .reply(reply)
                    .elapsedMs(elapsed)
                    .promptTokens(promptTokens != null ? promptTokens : request.getQuestion().length() / 4 + 1)
                    .completionTokens(completionTokens != null ? completionTokens : reply.length() / 4 + 1)
                    .build();

        } catch (Exception e) {
            throw new BusinessException("AI 模型调用失败: " + e.getMessage());
        }
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
        config.setTopP(modelConfigRequest.getTopP());
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
