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
import com.sspku.agent.module.knowledge.dto.RagConfig;
import com.sspku.agent.module.knowledge.service.RagService;
import com.sspku.agent.module.knowledge.mapper.AgentKnowledgeRelationMapper;
import com.sspku.agent.module.user.entity.User;
import com.sspku.agent.module.agent.tool.PluginToolFactory;
import com.sspku.agent.module.workflow.service.WorkflowService;
import com.sspku.agent.module.workflow.dto.WorkflowDebugRequest;
import com.sspku.agent.module.workflow.vo.WorkflowDebugResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.context.annotation.Lazy;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能体服务实现
 */
@Slf4j
@Service
public class AgentServiceImpl implements AgentService {

    private final AgentMapper agentMapper;
    private final AgentPluginRelationMapper agentPluginRelationMapper;
    private final UserAgentRelationMapper userAgentRelationMapper;
    private final UserPluginRelationMapper userPluginRelationMapper;
    private final ObjectMapper objectMapper;
    private final PluginToolFactory pluginToolFactory;
    private final RagService ragService;
    private final AgentKnowledgeRelationMapper agentKnowledgeRelationMapper;
    private final WorkflowService workflowService;

    // Spring AI 聊天模型（使用自动配置的默认模型）
    private final ChatModel chatModel;

    public AgentServiceImpl(
            AgentMapper agentMapper,
            AgentPluginRelationMapper agentPluginRelationMapper,
            UserAgentRelationMapper userAgentRelationMapper,
            UserPluginRelationMapper userPluginRelationMapper,
            ObjectMapper objectMapper,
            PluginToolFactory pluginToolFactory,
            RagService ragService,
            AgentKnowledgeRelationMapper agentKnowledgeRelationMapper,
            @Lazy WorkflowService workflowService,
            ChatModel chatModel) {
        this.agentMapper = agentMapper;
        this.agentPluginRelationMapper = agentPluginRelationMapper;
        this.userAgentRelationMapper = userAgentRelationMapper;
        this.userPluginRelationMapper = userPluginRelationMapper;
        this.objectMapper = objectMapper;
        this.pluginToolFactory = pluginToolFactory;
        this.ragService = ragService;
        this.agentKnowledgeRelationMapper = agentKnowledgeRelationMapper;
        this.workflowService = workflowService;
        this.chatModel = chatModel;
    }

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
        if (request.getRagConfig() != null) {
            try {
                agent.setRagConfig(objectMapper.writeValueAsString(request.getRagConfig()));
            } catch (JsonProcessingException e) {
                throw new BusinessException("RAG配置序列化失败");
            }
        }
        agent.setWorkflowId(request.getWorkflowId());
        agent.setStatus("draft");
        
        log.info("创建智能体: name={}, workflowId={}", request.getName(), request.getWorkflowId());

        agentMapper.insert(agent);
        userAgentRelationMapper.upsertOwner(ownerUserId, agent.getId());
        saveAgentPlugins(agent.getId(), request.getPluginIds());
        saveAgentKnowledgeBase(agent.getId(), request.getKnowledgeBaseId());
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

        if (request.getRagConfig() != null) {
            try {
                agent.setRagConfig(objectMapper.writeValueAsString(request.getRagConfig()));
            } catch (JsonProcessingException e) {
                throw new BusinessException("RAG配置序列化失败");
            }
        }

        // 处理 workflowId：总是更新（包括设置为 null 的情况）
        // 注意：由于 Java 无法区分 null 和未设置，这里简化处理：总是更新 workflowId
        // 如果需要清空关联，前端需要显式传入 null
        Long oldWorkflowId = agent.getWorkflowId();
        agent.setWorkflowId(request.getWorkflowId());
        log.info("更新智能体 {} 的 workflowId: {} -> {}", id, oldWorkflowId, request.getWorkflowId());

        if (request.getKnowledgeBaseId() != null) {
            saveAgentKnowledgeBase(id, request.getKnowledgeBaseId());
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
        // 删除关联的知识库关系
        agentKnowledgeRelationMapper.deleteByAgentId(id);
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

        // 如果配置了工作流，优先使用工作流执行
        if (agent.getWorkflowId() != null) {
            try {
                // 构建工作流输入
                Map<String, Object> workflowInputs = new HashMap<>();
                workflowInputs.put("query", request.getQuestion());
                
                // 如果有会话历史，转换为工作流输入格式
                if (request.getMessages() != null && !request.getMessages().isEmpty()) {
                    List<Map<String, Object>> messagesList = new ArrayList<>();
                    for (var msg : request.getMessages()) {
                        Map<String, Object> msgMap = new HashMap<>();
                        msgMap.put("role", msg.getRole());
                        msgMap.put("content", msg.getContent());
                        if (msg.getTimestamp() != null) {
                            msgMap.put("timestamp", msg.getTimestamp());
                        }
                        messagesList.add(msgMap);
                    }
                    workflowInputs.put("messages", messagesList);
                }
                
                // 执行工作流
                WorkflowDebugRequest workflowRequest = new WorkflowDebugRequest();
                workflowRequest.setInputs(workflowInputs);
                WorkflowDebugResponse workflowResponse = workflowService.executeWorkflow(
                    agent.getWorkflowId(), workflowRequest);
                
                // 提取工作流输出
                long workflowStart = Instant.now().toEpochMilli();
                String reply = extractWorkflowOutput(workflowResponse);
                long elapsed = Instant.now().toEpochMilli() - workflowStart;
                
                return AgentTestResponse.builder()
                    .reply(reply)
                    .elapsedMs((int) elapsed)
                    .promptTokens(0)  // 工作流执行不统计 token
                    .completionTokens(0)
                    .build();
                    
            } catch (Exception e) {
                // 工作流执行失败，直接抛出错误，不回退
                log.error("工作流执行失败: workflowId={}, error={}", agent.getWorkflowId(), e.getMessage(), e);
                throw new BusinessException("工作流执行失败: " + e.getMessage());
            }
        } else {
            log.info("智能体 {} 使用传统 LLM 模式执行（未配置工作流）", id);
        }

        // 无工作流，执行传统 LLM 逻辑
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

            // 如果前端传入了会话历史，则优先使用历史构建消息
            if (request.getMessages() != null && !request.getMessages().isEmpty()) {
                // 仅对最后一条 user 消息做 RAG 增强
                int lastUserIndex = -1;
                for (int i = request.getMessages().size() - 1; i >= 0; i--) {
                    var m = request.getMessages().get(i);
                    if ("user".equalsIgnoreCase(m.getRole())) {
                        lastUserIndex = i;
                        break;
                    }
                }

                for (int i = 0; i < request.getMessages().size(); i++) {
                    var m = request.getMessages().get(i);
                    if (m == null || !StringUtils.hasText(m.getRole()) || !StringUtils.hasText(m.getContent())) {
                        continue;
                    }
                    String role = m.getRole().toLowerCase(Locale.ROOT);
                    switch (role) {
                        case "user": {
                            String content = m.getContent();
                            if (i == lastUserIndex) {
                                try {
                                    List<KnowledgeChunk> chunks = ragService.retrieve(id, content,
                                            request.getRagConfig());
                                    if (!CollectionUtils.isEmpty(chunks)) {
                                        content = ragService.buildPrompt(id, content, chunks, request.getRagConfig());
                                    }
                                } catch (Exception e) {
                                    // RAG 失败不影响主流程
                                    log.warn("RAG retrieve/build failed: {}", e.getMessage());
                                }
                            }
                            messages.add(new UserMessage(content));
                            break;
                        }
                        case "assistant": {
                            messages.add(new AssistantMessage(m.getContent()));
                            break;
                        }
                        default: {
                            // 未知角色，跳过
                        }
                    }
                }
            } else {
                // 否则退化为仅当前问题 + 可选 RAG 增强
                String userQuestion = request.getQuestion();
                try {
                    List<KnowledgeChunk> chunks = ragService.retrieve(id, userQuestion, request.getRagConfig());
                    if (!CollectionUtils.isEmpty(chunks)) {
                        userQuestion = ragService.buildPrompt(id, userQuestion, chunks, request.getRagConfig());
                    }
                } catch (Exception e) {
                    // RAG 失败不影响主流程
                    log.warn("RAG retrieve/build failed: {}", e.getMessage());
                }
                messages.add(new UserMessage(userQuestion));
            }

            // 获取绑定的插件并转换为 ToolCallback
            List<Long> pluginIds = agentPluginRelationMapper.selectPluginIdsByAgentId(id);
            List<ToolCallback> toolCallbacks = pluginToolFactory.createToolCallbacks(pluginIds);

            // 使用智能体配置的参数创建运行时 ChatOptions
            var optionsBuilder = org.springframework.ai.model.tool.ToolCallingChatOptions
                    .builder()
                    .model(modelConfig.getModel())
                    .temperature(modelConfig.getTemperature() != null ? modelConfig.getTemperature() : 0.7)
                    .topP(modelConfig.getTopP() != null ? modelConfig.getTopP() : 0.9);

            if (modelConfig.getMaxTokens() != null) {
                optionsBuilder.maxTokens(modelConfig.getMaxTokens());
            }

            if (!CollectionUtils.isEmpty(toolCallbacks)) {
                optionsBuilder.toolCallbacks(toolCallbacks);
            }

            var options = optionsBuilder.build();

            // 创建带有选项的 Prompt
            Prompt prompt = new Prompt(messages, options);

            // 调用模型
            ChatClient chatClient = ChatClient.builder(chatModel).build();
            ChatResponse response = chatClient.prompt(prompt).call().chatResponse();

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

    private RagConfig readRagConfig(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, RagConfig.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse RAG config", e);
            return null;
        }
    }

    /**
     * 从工作流响应中提取输出
     */
    private String extractWorkflowOutput(WorkflowDebugResponse response) {
        if (response == null || response.getOutput() == null) {
            throw new BusinessException("工作流执行失败：未返回输出");
        }
        
        Object output = response.getOutput();
        
        // 如果输出是 Map，尝试提取常见的输出键
        if (output instanceof Map<?, ?> outputMap) {
            // 优先查找 "answer"
            Object answer = outputMap.get("answer");
            if (answer != null) {
                return String.valueOf(answer);
            }
            
            // 其次查找 "result"
            Object result = outputMap.get("result");
            if (result != null) {
                return String.valueOf(result);
            }
            
            // 如果只有一个键值对，返回该值
            if (outputMap.size() == 1) {
                return String.valueOf(outputMap.values().iterator().next());
            }
            
            // 否则返回整个输出的 JSON 字符串
            try {
                return objectMapper.writeValueAsString(output);
            } catch (JsonProcessingException e) {
                return output.toString();
            }
        }
        
        // 如果输出是字符串，直接返回
        return String.valueOf(output);
    }

    private Long getKnowledgeBaseId(Long agentId) {
        List<Long> kbIds = agentKnowledgeRelationMapper.selectKbIdsByAgentId(agentId);
        if (CollectionUtils.isEmpty(kbIds)) {
            return null;
        }
        // 约定单选知识库：取最新关联的一条
        return kbIds.get(0);
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
                .knowledgeBaseId(getKnowledgeBaseId(agent.getId()))
                .ragConfig(readRagConfig(agent.getRagConfig()))
                .workflowId(agent.getWorkflowId())
                .status(agent.getStatus())
                .createdAt(agent.getCreatedAt())
                .updatedAt(agent.getUpdatedAt())
                .build();
    }

    private void saveAgentKnowledgeBase(Long agentId, Long knowledgeBaseId) {
        agentKnowledgeRelationMapper.deleteByAgentId(agentId);
        if (knowledgeBaseId != null) {
            agentKnowledgeRelationMapper.insertBatch(agentId, Collections.singletonList(knowledgeBaseId));
        }
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
