package com.sspku.agent.module.workflow.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sspku.agent.common.api.PageResponse;
import com.sspku.agent.common.exception.BusinessException;
import com.sspku.agent.module.user.entity.User;
import com.sspku.agent.module.workflow.dto.WorkflowCreateRequest;
import com.sspku.agent.module.workflow.dto.WorkflowDebugRequest;
import com.sspku.agent.module.workflow.dto.WorkflowListQuery;
import com.sspku.agent.module.workflow.dto.WorkflowUpdateRequest;
import com.sspku.agent.module.workflow.entity.Workflow;
import com.sspku.agent.module.workflow.mapper.WorkflowMapper;
import com.sspku.agent.module.workflow.model.WorkflowEdge;
import com.sspku.agent.module.workflow.model.WorkflowGraph;
import com.sspku.agent.module.workflow.model.WorkflowNode;
import com.sspku.agent.module.workflow.service.WorkflowService;
import com.sspku.agent.module.workflow.vo.WorkflowDebugResponse;
import com.sspku.agent.module.workflow.vo.WorkflowTraceEvent;
import com.sspku.agent.module.workflow.vo.WorkflowVO;
import com.sspku.agent.module.knowledge.entity.KnowledgeChunk;
import com.sspku.agent.module.knowledge.service.RagService;
import com.sspku.agent.module.agent.tool.PluginToolFactory;
import com.sspku.agent.module.agent.service.AgentService;
import com.sspku.agent.module.agent.dto.AgentTestRequest;
import com.sspku.agent.module.agent.dto.ConversationMessage;
import com.sspku.agent.module.agent.vo.AgentTestResponse;
import com.sspku.agent.module.knowledge.dto.RagConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private static final Pattern INPUT_VAR_PATTERN = Pattern
            .compile("\\{\\{\\s*inputs\\.([a-zA-Z0-9_\\.]+)\\s*\\}\\}");

    private final WorkflowMapper workflowMapper;
    private final ObjectMapper objectMapper;
    private final ChatModel chatModel;
    private final RagService ragService;
    private final PluginToolFactory pluginToolFactory;
    private final AgentService agentService;

    // Thread local storage for workflow graph
    private final ThreadLocal<WorkflowGraph> workflowGraphThreadLocal = new ThreadLocal<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWorkflow(WorkflowCreateRequest request) {
        Long ownerId = currentUserId();
        // Validate graph is JSON
        parseGraph(request.getGraph());

        Workflow workflow = new Workflow();
        workflow.setOwnerUserId(ownerId);
        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());
        workflow.setStatus("draft");
        workflow.setVersion("1.0");
        workflow.setGraph(request.getGraph());

        workflowMapper.insert(workflow);
        return workflow.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkflow(Long id, WorkflowUpdateRequest request) {
        Long ownerId = currentUserId();
        Workflow existing = workflowMapper.selectByIdAndOwner(id, ownerId);
        if (existing == null) {
            throw new BusinessException("工作流不存在");
        }

        Workflow update = new Workflow();
        update.setId(id);
        update.setOwnerUserId(ownerId);
        update.setName(request.getName());
        update.setDescription(request.getDescription());
        if (request.getGraph() != null) {
            parseGraph(request.getGraph());
            update.setGraph(request.getGraph());
        }

        workflowMapper.update(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkflow(Long id) {
        Long ownerId = currentUserId();
        int deleted = workflowMapper.deleteByIdAndOwner(id, ownerId);
        if (deleted <= 0) {
            throw new BusinessException("工作流不存在");
        }
    }

    @Override
    public WorkflowVO getWorkflow(Long id) {
        Long ownerId = currentUserId();
        Workflow wf = workflowMapper.selectByIdAndOwner(id, ownerId);
        if (wf == null) {
            throw new BusinessException("工作流不存在");
        }
        return toVO(wf);
    }

    @Override
    public PageResponse<WorkflowVO> listWorkflows(WorkflowListQuery query) {
        Long ownerId = currentUserId();

        int pageNo = query.getPageNo() != null && query.getPageNo() > 0 ? query.getPageNo() : 1;
        int pageSize = query.getPageSize() != null && query.getPageSize() > 0 ? query.getPageSize() : 20;
        int offset = (pageNo - 1) * pageSize;

        long total = workflowMapper.countByCondition(ownerId, query.getKeyword(), query.getStatus());
        if (total == 0) {
            return PageResponse.empty(pageNo, pageSize);
        }

        List<Workflow> items = workflowMapper.selectPageByCondition(ownerId, query.getKeyword(), query.getStatus(),
                pageSize, offset);
        List<WorkflowVO> vos = items.stream().map(this::toVO).collect(Collectors.toList());
        return PageResponse.of(total, pageNo, pageSize, vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishWorkflow(Long id) {
        Long ownerId = currentUserId();
        Workflow wf = workflowMapper.selectByIdAndOwner(id, ownerId);
        if (wf == null) {
            throw new BusinessException("工作流不存在");
        }
        if (!StringUtils.hasText(wf.getName())) {
            throw new BusinessException("请填写工作流名称");
        }
        validateGraphBasic(wf.getGraph());
        workflowMapper.updateStatus(id, ownerId, "published");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpublishWorkflow(Long id) {
        Long ownerId = currentUserId();
        int updated = workflowMapper.updateStatus(id, ownerId, "draft");
        if (updated <= 0) {
            throw new BusinessException("工作流不存在");
        }
    }

    @Override
    public WorkflowDebugResponse executeWorkflow(Long id, WorkflowDebugRequest request) {
        Workflow wf = workflowMapper.selectById(id);
        if (wf == null) {
            throw new BusinessException("工作流不存在");
        }
        
        // 检查工作流状态，必须为 published
        if (!"published".equals(wf.getStatus())) {
            throw new BusinessException("只能执行已发布的工作流，当前状态: " + wf.getStatus());
        }
        
        return executeWorkflowInternal(wf, request);
    }

    @Override
    public WorkflowDebugResponse debugWorkflow(Long id, WorkflowDebugRequest request) {
        Long ownerId = currentUserId();
        Workflow wf = workflowMapper.selectByIdAndOwner(id, ownerId);
        if (wf == null) {
            throw new BusinessException("工作流不存在");
        }
        
        return executeWorkflowInternal(wf, request);
    }
    
    /**
     * 工作流执行的核心逻辑（内部方法）
     */
    private WorkflowDebugResponse executeWorkflowInternal(Workflow wf, WorkflowDebugRequest request) {
        WorkflowGraph graph = parseGraph(wf.getGraph());
        validateGraphBasic(wf.getGraph());
        
        // Set workflow graph to thread local for parallel node execution
        workflowGraphThreadLocal.set(graph);

        Map<String, Object> inputs = request != null ? request.getInputs() : null;
        if (inputs == null)
            inputs = Collections.emptyMap();

        List<WorkflowTraceEvent> trace = new ArrayList<>();

        // Runtime context
        Map<String, Object> vars = new HashMap<>();
        Map<String, Object> nodeOutputs = new HashMap<>();
        vars.putAll(inputs);

        List<WorkflowNode> executionOrder = resolveExecutionOrder(graph);
        Object finalOutput = null;

        Map<String, String> conditionNodeResults = new HashMap<>();
        Set<String> executedNodeIds = new HashSet<>();

        for (WorkflowNode node : executionOrder) {
            String normalizedType = normalizeType(node.getType());
            WorkflowTraceEvent event;

            if (!shouldExecuteNode(node, graph, conditionNodeResults)) {
                continue;
            }
            
            // Skip nodes that have already been executed (e.g., in loop body)
            if (executedNodeIds.contains(node.getId())) {
                continue;
            }

            switch (normalizedType) {
                case "start":
                    event = runStartNode(node, inputs);
                    nodeOutputs.put(node.getId(), event.getOutput());
                    executedNodeIds.add(node.getId());
                    break;
                case "knowledgeRetrieval":
                    event = runKnowledgeRetrievalNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        // merge into vars
                        for (Map.Entry<?, ?> e : outMap.entrySet()) {
                            if (e.getKey() != null) {
                                vars.put(String.valueOf(e.getKey()), e.getValue());
                            }
                        }
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    executedNodeIds.add(node.getId());
                    break;
                case "textConcat":
                    event = runTextConcatenationNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        for (Map.Entry<?, ?> e : outMap.entrySet()) {
                            if (e.getKey() != null) {
                                vars.put(String.valueOf(e.getKey()), e.getValue());
                            }
                        }
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    executedNodeIds.add(node.getId());
                    break;
                case "variableAggregation":
                    event = runVariableAggregationNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        for (Map.Entry<?, ?> e : outMap.entrySet()) {
                            if (e.getKey() != null) {
                                vars.put(String.valueOf(e.getKey()), e.getValue());
                            }
                        }
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    executedNodeIds.add(node.getId());
                    break;
                case "variableUpdater":
                    event = runVariableUpdaterNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        for (Map.Entry<?, ?> e : outMap.entrySet()) {
                            if (e.getKey() != null) {
                                vars.put(String.valueOf(e.getKey()), e.getValue());
                            }
                        }
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    executedNodeIds.add(node.getId());
                    break;
                case "llm":
                    event = runLlmNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        String outputKey = StringUtils.hasText(node.getLlmOutputKey()) ? node.getLlmOutputKey()
                                : "llmOutput";
                        Object text = outMap.get("text");
                        vars.put(outputKey, text);
                        // keep a common alias
                        vars.put("llmOutput", text);
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    executedNodeIds.add(node.getId());
                    break;
                case "end":
                    event = runEndNode(node, vars);
                    nodeOutputs.put(node.getId(), event.getOutput());
                    finalOutput = event.getOutput();
                    executedNodeIds.add(node.getId());
                    break;
                case "textToImage":
                    event = runTextToImageNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        String outputKey = StringUtils.hasText(node.getTextToImageOutputKey()) ? node.getTextToImageOutputKey() : "imageUrl";
                        vars.put(outputKey, outMap.get("url"));
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    executedNodeIds.add(node.getId());
                    break;
                case "intelligentForm":
                    event = runIntelligentFormNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        String outputKey = StringUtils.hasText(node.getIntelligentFormOutputKey()) ? node.getIntelligentFormOutputKey() : "formData";
                        vars.put(outputKey, outMap.get("formData"));
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    executedNodeIds.add(node.getId());
                    break;
                case "condition":
                    event = runConditionNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        Map<String, Object> conditionResult = (Map<String, Object>) outMap.get("conditionResult");
                        vars.put("conditionResult", conditionResult);
                        if (conditionResult != null) {
                            String branch = (String) conditionResult.get("branch");
                            if (branch != null) {
                                conditionNodeResults.put(node.getId(), branch);
                            }
                        }
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    executedNodeIds.add(node.getId());
                    break;
                case "http":
                    event = runHttpNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        String outputKey = StringUtils.hasText(node.getHttpOutputKey()) ? node.getHttpOutputKey() : "httpResponse";
                        vars.put(outputKey, outMap.get("httpResponse"));
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    executedNodeIds.add(node.getId());
                    break;
                case "code":
                    event = runCodeNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        String outputKey = StringUtils.hasText(node.getCodeOutputKey()) ? node.getCodeOutputKey() : "codeOutput";
                        vars.put(outputKey, outMap.get("codeOutput"));
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    executedNodeIds.add(node.getId());
                    break;
                case "loop":
                    event = runLoopNode(node, inputs, vars, trace, executedNodeIds);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        String outputKey = StringUtils.hasText(node.getLoopOutputKey()) ? node.getLoopOutputKey() : "loopOutput";
                        vars.put(outputKey, outMap.get("loopOutput"));
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    executedNodeIds.add(node.getId());
                    break;
                case "parallel":
                    event = runParallelNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        String outputKey = StringUtils.hasText(node.getParallelOutputKey()) ? node.getParallelOutputKey() : "parallelOutput";
                        vars.put(outputKey, outMap.get("parallelOutput"));
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    executedNodeIds.add(node.getId());
                    break;
                case "manualCheck":
                    event = runManualCheckNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        vars.put("manualCheckResult", outMap.get("manualCheckResult"));
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    executedNodeIds.add(node.getId());
                    break;
                case "reply":
                    event = runReplyNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        vars.put("replyResult", outMap.get("replyResult"));
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    executedNodeIds.add(node.getId());
                    break;
                case "toolInvoke":
                    event = runToolInvokeNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        String outputVar = StringUtils.hasText(node.getToolOutputVar()) ? node.getToolOutputVar() : "toolOutput";
                        vars.put(outputVar, outMap.get("toolOutput"));
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    executedNodeIds.add(node.getId());
                    break;
                case "fileExtraction":
                    event = runFileExtractionNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        String outputKey = StringUtils.hasText(node.getFileExtractionOutputKey()) ? node.getFileExtractionOutputKey() : "fileContent";
                        vars.put(outputKey, outMap.get("fileContent"));
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    executedNodeIds.add(node.getId());
                    break;
                case "questionClassification":
                    event = runQuestionClassificationNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        String outputKey = StringUtils.hasText(node.getQuestionClassificationOutputKey()) ? node.getQuestionClassificationOutputKey() : "classification";
                        vars.put(outputKey, outMap.get("classification"));
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    break;
                case "queryOptimization":
                    event = runQueryOptimizationNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        String outputKey = StringUtils.hasText(node.getQueryOptimizationOutputKey()) ? node.getQueryOptimizationOutputKey() : "optimizedQuery";
                        vars.put(outputKey, outMap.get("optimizedQuery"));
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    break;
                case "textExtraction":
                    event = runTextExtractionNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        String outputKey = StringUtils.hasText(node.getTextExtractionOutputKey()) ? node.getTextExtractionOutputKey() : "extractedText";
                        vars.put(outputKey, outMap.get("extractedText"));
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    break;
                case "evaluationAlgorithms":
                    event = runEvaluationAlgorithmsNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        String outputKey = StringUtils.hasText(node.getEvaluationAlgorithmsOutputKey()) ? node.getEvaluationAlgorithmsOutputKey() : "evaluationResult";
                        vars.put(outputKey, outMap.get("evaluationResult"));
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    break;
                case "evaluationTestSet":
                    event = runEvaluationTestSetNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        String outputKey = StringUtils.hasText(node.getEvaluationTestSetOutputKey()) ? node.getEvaluationTestSetOutputKey() : "testSetResult";
                        vars.put(outputKey, outMap.get("testSetResult"));
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    break;
                case "evaluationStart":
                    event = runEvaluationStartNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        vars.put("evaluationStartResult", outMap.get("evaluationStartResult"));
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    break;
                case "evaluationEnd":
                    event = runEvaluationEndNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        String outputKey = StringUtils.hasText(node.getEvaluationEndOutputKey()) ? node.getEvaluationEndOutputKey() : "evaluationEndResult";
                        vars.put(outputKey, outMap.get("evaluationEndResult"));
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    break;

                case "extractor":
                    event = runExtractorNode(node, inputs, vars);
                    if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                        String outputKey = StringUtils.hasText(node.getExtractorOutputKey()) ? node.getExtractorOutputKey() : "extractedData";
                        vars.put(outputKey, outMap.get("extractedData"));
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    break;
                default:
                    event = WorkflowTraceEvent.builder()
                            .nodeId(node.getId())
                            .nodeType(node.getType())
                            .status("error")
                            .startedAt(Instant.now().toEpochMilli())
                            .finishedAt(Instant.now().toEpochMilli())
                            .input(Map.of("inputs", inputs, "vars", vars))
                            .error("Unsupported node type: " + node.getType())
                            .build();
            }

            trace.add(event);
            if ("error".equals(event.getStatus())) {
                break;
            }
        }

        return WorkflowDebugResponse.builder()
                .output(finalOutput)
                .trace(trace)
                .build();
    }

    private WorkflowTraceEvent runStartNode(WorkflowNode start, Map<String, Object> inputs) {
        long started = Instant.now().toEpochMilli();
        long finished = started;
        return WorkflowTraceEvent.builder()
                .nodeId(start.getId())
                .nodeType(start.getType())
                .status("success")
                .startedAt(started)
                .finishedAt(finished)
                .input(Map.of("inputs", inputs))
                .output(Map.of("inputs", inputs))
                .build();
    }

    private WorkflowTraceEvent runLlmNode(WorkflowNode llm, Map<String, Object> inputs, Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            // 1. 构建消息列表和内容映射（用于后续转换为 ConversationMessage）
            List<Message> messages = new ArrayList<>();
            List<String> messageContents = new ArrayList<>();  // 保存原始内容
            List<String> messageRoles = new ArrayList<>();      // 保存角色
            
            // 2. 添加 System Prompt（如果配置了）
            String systemPromptText = null;
            if (StringUtils.hasText(llm.getSystemPrompt())) {
                String systemPromptTemplate = llm.getSystemPrompt();
                validateVariables(systemPromptTemplate, inputs, vars);
                systemPromptText = renderTemplate(systemPromptTemplate, inputs, vars);
                messages.add(new SystemMessage(systemPromptText));
                messageContents.add(systemPromptText);
                messageRoles.add("system");
            }
            
            // 3. 处理会话历史（从 inputs.messages 或 vars.messages）
            Object messagesRaw = inputs.get("messages");
            if (messagesRaw == null) {
                messagesRaw = vars.get("messages");
            }
            
            if (messagesRaw instanceof List<?> messageList) {
                for (Object msg : messageList) {
                    if (msg instanceof Map<?, ?> msgMap) {
                        String role = String.valueOf(msgMap.get("role"));
                        Object contentObj = msgMap.get("content");
                        if (contentObj == null) {
                            continue;
                        }
                        String content = String.valueOf(contentObj);
                        if ("user".equalsIgnoreCase(role)) {
                            messages.add(new UserMessage(content));
                            messageContents.add(content);
                            messageRoles.add("user");
                        } else if ("assistant".equalsIgnoreCase(role)) {
                            messages.add(new AssistantMessage(content));
                            messageContents.add(content);
                            messageRoles.add("assistant");
                        }
                    }
                }
            }
            
            // 4. 添加当前 prompt（作为最后一条 user 消息）
            String template = StringUtils.hasText(llm.getPrompt()) ? llm.getPrompt() : "请回答：{{inputs.query}}";
            validateVariables(template, inputs, vars);
            String promptText = renderTemplate(template, inputs, vars);
            messages.add(new UserMessage(promptText));
            messageContents.add(promptText);
            messageRoles.add("user");

            Map<String, Object> outputData = new HashMap<>();
            String reply;
            long duration;

            // Check if direct model selection is used
            String modelProvider = llm.getModelProvider();
            String modelName = llm.getModelName();
            
            if (StringUtils.hasText(modelProvider) && StringUtils.hasText(modelName)) {
                // Direct model call with multi-turn conversation support
                long modelStarted = Instant.now().toEpochMilli();
                
                // Use Spring AI ChatModel directly with multi-turn messages
                Prompt prompt = new Prompt(messages);
                ChatResponse chatResponse = chatModel.call(prompt);
                // 提取文本内容，而不是对象的字符串表示
                reply = chatResponse.getResult().getOutput().getText();
                
                duration = Instant.now().toEpochMilli() - modelStarted;
                
                outputData.put("text", reply);
                outputData.put("modelProvider", modelProvider);
                outputData.put("modelName", modelName);
                outputData.put("duration", duration);
                outputData.put("messagesCount", messages.size());
            } else {
                // Fallback to agent-based call
                Long agentId = llm.getLlmAgentId();
                if (agentId == null) {
                    throw new BusinessException("未选择大模型智能体或模型参数");
                }
                
                // 构建 AgentTestRequest，支持多轮对话
                AgentTestRequest request = new AgentTestRequest();
                request.setQuestion(promptText);
                request.setRagConfig(new RagConfig());
                
                // 如果有历史消息，转换为 AgentTestRequest 的 messages 格式
                if (messageContents.size() > 1) {  // 除了最后一条 user 消息外还有其他消息
                    List<ConversationMessage> agentMessages = new ArrayList<>();
                    for (int i = 0; i < messageContents.size() - 1; i++) {  // 排除最后一条（当前 prompt）
                        String role = messageRoles.get(i);
                        String content = messageContents.get(i);
                        if ("system".equalsIgnoreCase(role)) {
                            // SystemMessage 会被 agent 的 systemPrompt 处理，这里跳过
                            continue;
                        } else if ("user".equalsIgnoreCase(role) || "assistant".equalsIgnoreCase(role)) {
                            ConversationMessage agentMsg = new ConversationMessage();
                            agentMsg.setRole(role);
                            agentMsg.setContent(content);
                            agentMessages.add(agentMsg);
                        }
                    }
                    if (!agentMessages.isEmpty()) {
                        request.setMessages(agentMessages);
                    }
                }

                AgentTestResponse response = agentService.testAgent(agentId, request);
                reply = response.getReply();
                duration = response.getElapsedMs();

                outputData.put("text", reply);
                outputData.put("agentId", agentId);
                outputData.put("duration", duration);
                outputData.put("messagesCount", messages.size());
            }

            long finished = Instant.now().toEpochMilli();
            
            return WorkflowTraceEvent.builder()
                    .nodeId(llm.getId())
                    .nodeType(llm.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("prompt", promptText, "messagesCount", messages.size()))
                    .output(outputData)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(llm.getId())
                    .nodeType(llm.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    private WorkflowTraceEvent runKnowledgeRetrievalNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            String queryTemplate = StringUtils.hasText(node.getQueryTemplate()) ? node.getQueryTemplate()
                    : "{{inputs.query}}";
            
            // 校验变量
            validateVariables(queryTemplate, inputs, vars);
            
            String query = renderTemplate(queryTemplate, inputs, vars);

            Long agentId = node.getAgentId();
            if (agentId == null) {
                String agentIdKey = StringUtils.hasText(node.getAgentIdKey()) ? node.getAgentIdKey() : "agentId";
                Object agentIdRaw = inputs.get(agentIdKey);
                if (agentIdRaw instanceof Number n) {
                    agentId = n.longValue();
                } else if (agentIdRaw != null) {
                    try {
                        agentId = Long.parseLong(String.valueOf(agentIdRaw));
                    } catch (Exception ignore) {
                    }
                }
            }

            String outputKey = StringUtils.hasText(node.getKnowledgeOutputKey()) ? node.getKnowledgeOutputKey()
                    : "knowledge";

            String knowledgeText = "";
            if (agentId != null && StringUtils.hasText(query)) {
                List<KnowledgeChunk> chunks = ragService.retrieve(agentId, query);
                if (chunks != null && !chunks.isEmpty()) {
                    knowledgeText = chunks.stream()
                            .filter(Objects::nonNull)
                            .map(KnowledgeChunk::getContent)
                            .filter(StringUtils::hasText)
                            .limit(6)
                            .collect(Collectors.joining("\n\n"));
                }
            }

            long finished = Instant.now().toEpochMilli();
            Map<String, Object> out = Map.of(outputKey, knowledgeText);
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("query", query, "agentId", agentId))
                    .output(out)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    private WorkflowTraceEvent runTextConcatenationNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            String partsText = node.getPartsText() != null ? node.getPartsText() : "";
            String separator = node.getSeparator() != null ? node.getSeparator() : "\n";
            String outputKey = StringUtils.hasText(node.getTextOutputKey()) ? node.getTextOutputKey() : "text";

            // 校验所有 parts 中的变量
            if (StringUtils.hasText(partsText)) {
                for (String part : partsText.split("\\r?\\n")) {
                    validateVariables(part.trim(), inputs, vars);
                }
            }

            List<String> parts = Arrays.stream(partsText.split("\\r?\\n"))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(t -> renderTemplate(t, inputs, vars))
                    .collect(Collectors.toList());

            String result = String.join(separator, parts);
            long finished = Instant.now().toEpochMilli();
            Map<String, Object> out = Map.of(outputKey, result);
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("parts", parts, "separator", separator))
                    .output(out)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    private WorkflowTraceEvent runVariableAggregationNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars) {
        // MVP: same behavior as text concatenation, output key defaults to "result"
        long started = Instant.now().toEpochMilli();
        try {
            String partsText = node.getPartsText() != null ? node.getPartsText() : "";
            String separator = node.getSeparator() != null ? node.getSeparator() : "\n";
            String outputKey = StringUtils.hasText(node.getTextOutputKey()) ? node.getTextOutputKey() : "result";

            List<String> parts = Arrays.stream(partsText.split("\\r?\\n"))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(t -> renderTemplate(t, inputs, vars))
                    .collect(Collectors.toList());

            String result = String.join(separator, parts);
            long finished = Instant.now().toEpochMilli();
            Map<String, Object> out = Map.of(outputKey, result);
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("parts", parts, "separator", separator))
                    .output(out)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    private WorkflowTraceEvent runVariableUpdaterNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            String targetKey = StringUtils.hasText(node.getTargetKey()) ? node.getTargetKey() : "answer";
            String valueTemplate = StringUtils.hasText(node.getValueTemplate()) ? node.getValueTemplate()
                    : "{{vars.llmOutput}}";
            
            // 校验变量
            validateVariables(valueTemplate, inputs, vars);
            
            String value = renderTemplate(valueTemplate, inputs, vars);

            long finished = Instant.now().toEpochMilli();
            Map<String, Object> out = Map.of(targetKey, value);
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("targetKey", targetKey, "valueTemplate", valueTemplate))
                    .output(out)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    private WorkflowTraceEvent runEndNode(WorkflowNode end, Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            String outputKey = StringUtils.hasText(end.getOutputKey()) ? end.getOutputKey() : "answer";
            Object out = vars.get(outputKey);
            
            // 如果默认的 answer 没找到，尝试找 llmOutput 作为兜底
            if (out == null && "answer".equals(outputKey)) {
                out = vars.get("llmOutput");
                if (out != null) {
                    outputKey = "llmOutput";
                }
            }

            if (out == null) {
                throw new BusinessException("结束节点未找到输出变量: " + outputKey + "。请检查上游节点的输出变量名是否匹配。可用变量有: " + vars.keySet());
            }

            Map<String, Object> output = new HashMap<>();
            output.put(outputKey, out);
            return WorkflowTraceEvent.builder()
                    .nodeId(end.getId())
                    .nodeType(end.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(Instant.now().toEpochMilli())
                    .input(Map.of("outputKey", outputKey))
                    .output(output)
                    .build();
        } catch (Exception e) {
            return WorkflowTraceEvent.builder()
                    .nodeId(end.getId())
                    .nodeType(end.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(Instant.now().toEpochMilli())
                    .error(e.getMessage() != null ? e.getMessage() : "未知错误")
                    .build();
        }
    }

    private boolean shouldExecuteNode(WorkflowNode node, WorkflowGraph graph, Map<String, String> conditionNodeResults) {
        List<WorkflowEdge> edges = graph.getEdges() != null ? graph.getEdges() : Collections.emptyList();
        if (edges.isEmpty()) {
            return true;
        }

        for (WorkflowEdge edge : edges) {
            if (edge == null || !StringUtils.hasText(edge.getTo()) || !edge.getTo().equals(node.getId())) {
                continue;
            }

            if (!StringUtils.hasText(edge.getFrom())) {
                continue;
            }

            String fromNodeId = edge.getFrom();
            if (!conditionNodeResults.containsKey(fromNodeId)) {
                continue;
            }

            String edgeLabel = edge.getLabel();
            String conditionResult = conditionNodeResults.get(fromNodeId);

            if (StringUtils.hasText(edgeLabel)) {
                if (!StringUtils.hasText(conditionResult)) {
                    return false;
                }

                if ("true".equalsIgnoreCase(edgeLabel) && !"if".equals(conditionResult)) {
                    return false;
                }
                if ("false".equalsIgnoreCase(edgeLabel) && "if".equals(conditionResult)) {
                    return false;
                }
                if ("else".equalsIgnoreCase(edgeLabel) && !"else".equals(conditionResult)) {
                    return false;
                }
            }
        }

        return true;
    }

    private String normalizeType(String rawType) {
        if (!StringUtils.hasText(rawType))
            return "unknown";
        return switch (rawType) {
            case "start", "startNodeStart" -> "start";
            case "end", "endNodeEnd" -> "end";
            case "llm", "llmNodeState" -> "llm";
            case "knowledgeRetrievalNodeState" -> "knowledgeRetrieval";
            case "textConcatenationNodeState" -> "textConcat";
            case "variableAggregationNodeState" -> "variableAggregation";
            case "variableUpdaterNodeState" -> "variableUpdater";
            case "textToImageNodeState" -> "textToImage";
            case "intelligentFormNodeState" -> "intelligentForm";
            case "conditionNodeCondition" -> "condition";
            case "httpNodeState" -> "http";
            case "codeNodeState" -> "code";
            case "loopNodeState" -> "loop";
            case "parallelNodeState" -> "parallel";
            case "manualCheckNodeState" -> "manualCheck";
            case "replyNodeState" -> "reply";
            case "toolInvokeNodeState" -> "toolInvoke";
            case "fileExtractionNodeState" -> "fileExtraction";
            case "questionClassificationNodeState" -> "questionClassification";
            case "queryOptimizationNodeState" -> "queryOptimization";
            case "textExtractionNodeState" -> "textExtraction";
            case "evaluationAlgorithmsNodeState" -> "evaluationAlgorithms";
            case "evaluationTestSetNodeState" -> "evaluationTestSet";
            case "evaluationStartNodeState" -> "evaluationStart";
            case "evaluationEndNodeState" -> "evaluationEnd";

            case "extractorNodeState" -> "extractor";
            default -> rawType;
        };
    }

    private List<WorkflowNode> resolveExecutionOrder(WorkflowGraph graph) {
        List<WorkflowNode> nodes = graph.getNodes() != null ? graph.getNodes() : Collections.emptyList();
        if (nodes.isEmpty()) {
            throw new BusinessException("工作流图为空");
        }

        List<WorkflowEdge> edges = graph.getEdges() != null ? graph.getEdges() : Collections.emptyList();
        if (edges.isEmpty()) {
            // fallback: order by x,y
            return nodes.stream()
                    .sorted(Comparator
                            .comparing((WorkflowNode n) -> n.getX() == null ? 0.0 : n.getX())
                            .thenComparing(n -> n.getY() == null ? 0.0 : n.getY()))
                    .collect(Collectors.toList());
        }

        Map<String, WorkflowNode> byId = nodes.stream()
                .filter(n -> StringUtils.hasText(n.getId()))
                .collect(Collectors.toMap(WorkflowNode::getId, n -> n, (a, b) -> a));

        Map<String, Integer> indegree = new HashMap<>();
        Map<String, List<String>> adj = new HashMap<>();
        for (WorkflowNode n : nodes) {
            indegree.put(n.getId(), 0);
            adj.put(n.getId(), new ArrayList<>());
        }
        for (WorkflowEdge e : edges) {
            if (e == null || !StringUtils.hasText(e.getFrom()) || !StringUtils.hasText(e.getTo()))
                continue;
            if (!byId.containsKey(e.getFrom()) || !byId.containsKey(e.getTo()))
                continue;
            adj.get(e.getFrom()).add(e.getTo());
            indegree.put(e.getTo(), indegree.getOrDefault(e.getTo(), 0) + 1);
        }

        // Kahn topological sort
        Deque<String> q = new ArrayDeque<>();
        for (Map.Entry<String, Integer> kv : indegree.entrySet()) {
            if (kv.getValue() == 0)
                q.add(kv.getKey());
        }

        List<WorkflowNode> order = new ArrayList<>();
        while (!q.isEmpty()) {
            String id = q.removeFirst();
            order.add(byId.get(id));
            for (String to : adj.getOrDefault(id, Collections.emptyList())) {
                int nd = indegree.getOrDefault(to, 0) - 1;
                indegree.put(to, nd);
                if (nd == 0)
                    q.addLast(to);
            }
        }

        if (order.size() != byId.size()) {
            throw new BusinessException("工作流存在环或非法连线");
        }
        return order;
    }

    private String renderTemplate(String template, Map<String, Object> inputs, Map<String, Object> vars) {
        if (!StringUtils.hasText(template))
            return "";

        String result = template;

        // inputs.xxx
        Matcher m = INPUT_VAR_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String path = m.group(1);
            Object val = resolvePath(inputs, path);
            m.appendReplacement(sb, Matcher.quoteReplacement(val == null ? "" : String.valueOf(val)));
        }
        m.appendTail(sb);
        result = sb.toString();

        // vars.xxx
        Pattern varsPattern = Pattern.compile("\\{\\{\\s*vars\\.([a-zA-Z0-9_\\.]+)\\s*\\}\\}");
        Matcher mv = varsPattern.matcher(result);
        sb = new StringBuffer();
        while (mv.find()) {
            String path = mv.group(1);
            Object val = resolvePath(vars, path);
            mv.appendReplacement(sb, Matcher.quoteReplacement(val == null ? "" : String.valueOf(val)));
        }
        mv.appendTail(sb);
        return sb.toString();
    }

    private void validateVariables(String template, Map<String, Object> inputs, Map<String, Object> vars) {
        if (!StringUtils.hasText(template)) return;

        // 检查 inputs.xxx - 只记录警告，不抛出异常
        Matcher m = INPUT_VAR_PATTERN.matcher(template);
        while (m.find()) {
            String path = m.group(1);
            if (resolvePath(inputs, path) == null) {
                log.warn("输入变量 inputs.{} 不存在，将使用空字符串代替", path);
            }
        }

        // 检查 vars.xxx - 只记录警告，不抛出异常
        Pattern varsPattern = Pattern.compile("\\{\\{\\s*vars\\.([a-zA-Z0-9_\\.]+)\\s*\\}\\}");
        Matcher mv = varsPattern.matcher(template);
        while (mv.find()) {
            String path = mv.group(1);
            if (resolvePath(vars, path) == null) {
                log.warn("中间变量 vars.{} 不存在，将使用空字符串代替", path);
            }
        }
    }

    private Object resolvePath(Map<String, Object> root, String dottedPath) {
        if (root == null || !StringUtils.hasText(dottedPath))
            return null;
        Object cur = root;
        for (String p : dottedPath.split("\\.")) {
            if (!(cur instanceof Map<?, ?> m))
                return null;
            cur = m.get(p);
            if (cur == null)
                return null;
        }
        return cur;
    }

    private Object resolveVariableValue(String variable, Map<String, Object> inputs, Map<String, Object> vars) {
        if (!StringUtils.hasText(variable))
            return null;

        if (variable.startsWith("inputs.")) {
            String path = variable.substring("inputs.".length());
            return resolvePath(inputs, path);
        } else if (variable.startsWith("vars.")) {
            String path = variable.substring("vars.".length());
            return resolvePath(vars, path);
        } else {
            return resolvePath(vars, variable);
        }
    }

    private Object resolveConditionValue(Map<String, Object> valueConfig, Map<String, Object> inputs, Map<String, Object> vars) {
        if (valueConfig == null)
            return null;

        String from = (String) valueConfig.get("from");

        if ("Input".equalsIgnoreCase(from)) {
            return valueConfig.get("value");
        } else if ("Reference".equalsIgnoreCase(from)) {
            String referenceKey = (String) valueConfig.get("referenceKey");
            if (referenceKey != null) {
                return resolveVariableValue(referenceKey, inputs, vars);
            }
        }

        return valueConfig.get("value");
    }

    private boolean evaluateCondition(Object actualValue, String operator, Object expectedValue) {
        if (actualValue == null || expectedValue == null) {
            return "equal".equals(operator) && actualValue == expectedValue;
        }

        String actualStr = actualValue.toString();
        String expectedStr = expectedValue.toString();

        switch (operator) {
            case "equal":
            case "eq":
                return actualValue.equals(expectedValue);
            case "notEqual":
            case "ne":
                return !actualValue.equals(expectedValue);
            case "greaterThan":
            case "gt":
                return compareNumbers(actualValue, expectedValue) > 0;
            case "lessThan":
            case "lt":
                return compareNumbers(actualValue, expectedValue) < 0;
            case "greaterThanOrEqual":
            case "gte":
                return compareNumbers(actualValue, expectedValue) >= 0;
            case "lessThanOrEqual":
            case "lte":
                return compareNumbers(actualValue, expectedValue) <= 0;
            case "contains":
                return actualStr.contains(expectedStr);
            case "notContains":
                return !actualStr.contains(expectedStr);
            case "startsWith":
                return actualStr.startsWith(expectedStr);
            case "endsWith":
                return actualStr.endsWith(expectedStr);
            case "true":
                return Boolean.TRUE.toString().equalsIgnoreCase(actualStr);
            case "false":
                return Boolean.FALSE.toString().equalsIgnoreCase(actualStr);
            default:
                return false;
        }
    }

    private int compareNumbers(Object a, Object b) {
        try {
            double numA = a instanceof Number ? ((Number) a).doubleValue() : Double.parseDouble(a.toString());
            double numB = b instanceof Number ? ((Number) b).doubleValue() : Double.parseDouble(b.toString());
            return Double.compare(numA, numB);
        } catch (NumberFormatException e) {
            return a.toString().compareTo(b.toString());
        }
    }

    private WorkflowTraceEvent runEndNode(WorkflowNode end, String lastText) {
        long started = Instant.now().toEpochMilli();
        String outputKey = StringUtils.hasText(end.getOutputKey()) ? end.getOutputKey() : "answer";
        Map<String, Object> output = new LinkedHashMap<>();
        output.put(outputKey, lastText);
        long finished = Instant.now().toEpochMilli();
        return WorkflowTraceEvent.builder()
                .nodeId(end.getId())
                .nodeType(end.getType())
                .status("success")
                .startedAt(started)
                .finishedAt(finished)
                .input(Map.of("text", lastText))
                .output(output)
                .build();
    }

    private WorkflowTraceEvent runTextToImageNode(WorkflowNode node, Map<String, Object> inputs, Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            String promptTemplate = StringUtils.hasText(node.getTextToImagePrompt()) ? node.getTextToImagePrompt()
                    : "{{inputs.prompt}}";
            
            validateVariables(promptTemplate, inputs, vars);
            
            String prompt = renderTemplate(promptTemplate, inputs, vars);
            String outputKey = StringUtils.hasText(node.getTextToImageOutputKey()) ? node.getTextToImageOutputKey() : "imageUrl";

            if (!StringUtils.hasText(prompt)) {
                throw new BusinessException("文本转图片节点缺少提示词参数");
            }

            // Check if direct model selection is used
            String modelProvider = node.getTextToImageModelProvider();
            String modelName = node.getTextToImageModelName();
            Integer width = node.getTextToImageWidth();
            Integer height = node.getTextToImageHeight();
            
            long duration;
            String imageUrl;
            Map<String, Object> out;
            Long agentId = node.getTextToImageAgentId();
            
            if (StringUtils.hasText(modelProvider) && StringUtils.hasText(modelName)) {
                // Direct model call
                long modelStarted = Instant.now().toEpochMilli();
                
                // Use direct image generation API
                imageUrl = generateImage(prompt, modelName, width, height);
                
                duration = Instant.now().toEpochMilli() - modelStarted;
                
                out = Map.of(outputKey, imageUrl, "url", imageUrl, "prompt", prompt, "modelProvider", modelProvider, "modelName", modelName, "duration", duration);
            } else {
                // Fallback to agent-based call
                if (agentId == null) {
                    throw new BusinessException("未选择文本转图片智能体或模型参数");
                }

                AgentTestRequest request = new AgentTestRequest();
                request.setQuestion("请根据以下提示词生成图片: " + prompt);
                request.setRagConfig(new RagConfig());

                AgentTestResponse response = agentService.testAgent(agentId, request);
                imageUrl = response.getReply();
                duration = response.getElapsedMs();

                out = Map.of(outputKey, imageUrl, "url", imageUrl, "prompt", prompt, "agentId", agentId, "duration", duration);
            }

            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(StringUtils.hasText(modelProvider) ? Map.of("prompt", prompt, "modelProvider", modelProvider, "modelName", modelName) : Map.of("prompt", prompt, "agentId", agentId))
                    .output(out)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    private String generateImage(String prompt, String model, Integer width, Integer height) {
        try {
            String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
            if (!StringUtils.hasText(apiKey)) {
                apiKey = "sk-88487044811e4a699b2d43907b369979";
            }

            // Default size if not provided
            int imgWidth = width != null ? width : 1024;
            int imgHeight = height != null ? height : 1024;

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            
            String requestBody = String.format(
                "{\"model\":\"%s\",\"input\":{\"prompt\":\"%s\"},\"parameters\":{\"size\":\"%d*%d\"}}",
                model,
                prompt.replace("\"", "\\\""),
                imgWidth,
                imgHeight);

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis/generation"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            java.net.http.HttpResponse<String> response = client.send(request, 
                java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new BusinessException("图片生成API调用失败: " + response.statusCode() + " - " + response.body());
            }

            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode outputNode = responseJson.path("output");
            JsonNode resultsNode = outputNode.path("results");
            
            if (resultsNode.isArray() && resultsNode.size() > 0) {
                return resultsNode.get(0).path("url").asText();
            }

            throw new BusinessException("图片生成API返回结果格式错误");
        } catch (Exception e) {
            log.error("生成图片失败", e);
            throw new BusinessException("生成图片失败: " + e.getMessage());
        }
    }

    private WorkflowTraceEvent runIntelligentFormNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            String formId = node.getIntelligentFormFormId();
            String formVersion = node.getIntelligentFormFormVersion();
            Boolean enableStageDesc = node.getIntelligentFormEnableStageDesc() != null ? node.getIntelligentFormEnableStageDesc() : false;
            String stageDesc = node.getIntelligentFormStageDesc();
            String outputKey = StringUtils.hasText(node.getIntelligentFormOutputKey()) ? node.getIntelligentFormOutputKey() : "formData";

            if (!StringUtils.hasText(formId)) {
                throw new BusinessException("智能表单节点缺少表单ID参数");
            }

            Map<String, Object> formData = new HashMap<>();
            formData.put("formId", formId);
            formData.put("formVersion", formVersion != null ? formVersion : "1.0");
            formData.put("enableStageDesc", enableStageDesc);
            if (enableStageDesc && StringUtils.hasText(stageDesc)) {
                formData.put("stageDesc", stageDesc);
            }

            long finished = Instant.now().toEpochMilli();
            Map<String, Object> out;
            if ("formData".equals(outputKey)) {
                out = Map.of(outputKey, formData);
            } else {
                out = Map.of(outputKey, formData, "formData", formData);
            }
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .output(out)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    private WorkflowTraceEvent runConditionNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            Map<String, Object> conditionParams = node.getConditionParams();
            String outputKey = "conditionResult";

            if (conditionParams == null || conditionParams.isEmpty()) {
                throw new BusinessException("条件节点缺少条件参数");
            }

            List<Map<String, Object>> branches = (List<Map<String, Object>>) conditionParams.get("branches");
            if (branches == null || branches.isEmpty()) {
                throw new BusinessException("条件节点缺少分支配置");
            }

            boolean matched = false;
            String matchedBranch = null;
            Map<String, Object> matchedCondition = null;

            for (Map<String, Object> branch : branches) {
                String branchType = (String) branch.get("type");
                List<Map<String, Object>> conditions = (List<Map<String, Object>>) branch.get("conditions");

                if ("else".equalsIgnoreCase(branchType)) {
                    if (!matched) {
                        matched = true;
                        matchedBranch = "else";
                        break;
                    }
                } else if ("if".equalsIgnoreCase(branchType) && conditions != null) {
                    boolean branchMatched = true;
                    for (Map<String, Object> cond : conditions) {
                        String conditionType = (String) cond.get("condition");
                        List<Map<String, Object>> values = (List<Map<String, Object>>) cond.get("value");

                        if (values != null && values.size() >= 2) {
                            Object leftValue = resolveConditionValue(values.get(0), inputs, vars);
                            Object rightValue = resolveConditionValue(values.get(1), inputs, vars);

                            if (!evaluateCondition(leftValue, conditionType, rightValue)) {
                                branchMatched = false;
                                break;
                            }
                        }
                    }

                    if (branchMatched) {
                        matched = true;
                        matchedBranch = "if";
                        matchedCondition = branch;
                        break;
                    }
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("conditionParams", conditionParams);
            result.put("matched", matched);
            result.put("branch", matchedBranch != null ? matchedBranch : "false");
            result.put("matchedCondition", matchedCondition);

            long finished = Instant.now().toEpochMilli();
            Map<String, Object> out = Map.of(outputKey, result);
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .output(out)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    private WorkflowTraceEvent runHttpNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            String url = StringUtils.hasText(node.getHttpUrl()) ? renderTemplate(node.getHttpUrl(), inputs, vars) : "";
            String method = StringUtils.hasText(node.getHttpMethod()) ? node.getHttpMethod() : "GET";
            String headers = StringUtils.hasText(node.getHttpHeaders()) ? node.getHttpHeaders() : "{}";
            String body = StringUtils.hasText(node.getHttpBody()) ? renderTemplate(node.getHttpBody(), inputs, vars) : "";
            String outputKey = StringUtils.hasText(node.getHttpOutputKey()) ? node.getHttpOutputKey() : "httpResponse";

            if (!StringUtils.hasText(url)) {
                throw new BusinessException("HTTP节点缺少URL参数");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("url", url);
            response.put("method", method);
            response.put("headers", headers);
            response.put("body", body);
            response.put("status", 200);
            response.put("data", "HTTP请求已配置（需要实现实际的HTTP客户端）");

            long finished = Instant.now().toEpochMilli();
            Map<String, Object> out = Map.of(outputKey, response);
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .output(out)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    private WorkflowTraceEvent runCodeNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            String language = StringUtils.hasText(node.getCodeLanguage()) ? node.getCodeLanguage() : "javascript";
            String script = StringUtils.hasText(node.getCodeScript()) ? node.getCodeScript() : "";
            String outputKey = StringUtils.hasText(node.getCodeOutputKey()) ? node.getCodeOutputKey() : "codeResult";

            if (!StringUtils.hasText(script)) {
                throw new BusinessException("代码节点缺少脚本内容");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("language", language);
            result.put("script", script);
            result.put("output", "代码执行已配置（需要实现实际的代码执行引擎）");

            long finished = Instant.now().toEpochMilli();
            Map<String, Object> out;
            if ("codeResult".equals(outputKey)) {
                out = Map.of(outputKey, result);
            } else {
                out = Map.of(outputKey, result, "codeResult", result);
            }
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .output(out)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    private WorkflowTraceEvent runLoopNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars, List<WorkflowTraceEvent> trace, Set<String> executedNodeIds) {
        long started = Instant.now().toEpochMilli();
        try {
            String list = StringUtils.hasText(node.getLoopList()) ? renderTemplate(node.getLoopList(), inputs, vars) : "[]";
            String iterationVar = StringUtils.hasText(node.getLoopIterationVar()) ? node.getLoopIterationVar() : "item";
            Integer maxIterations = node.getLoopMaxIterations() != null ? node.getLoopMaxIterations() : 100;
            String outputKey = StringUtils.hasText(node.getLoopOutputKey()) ? node.getLoopOutputKey() : "loopResults";

            // Parse the list string into a JSON array
            JsonNode listArray = objectMapper.readTree(list);
            if (!listArray.isArray()) {
                throw new BusinessException("Loop list must be a JSON array");
            }

            // Get the current workflow graph from thread local
            WorkflowGraph graph = workflowGraphThreadLocal.get();
            if (graph == null) {
                throw new BusinessException("Workflow graph not found");
            }

            // Find all direct successor nodes of the loop node
            List<WorkflowEdge> edges = graph.getEdges() != null ? graph.getEdges() : Collections.emptyList();
            List<String> successorNodeIds = edges.stream()
                    .filter(e -> e.getFrom().equals(node.getId()))
                    .map(WorkflowEdge::getTo)
                    .distinct()
                    .collect(Collectors.toList());

            // If there are no direct successors, return empty results
            if (successorNodeIds.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("list", list);
                result.put("iterationVar", iterationVar);
                result.put("maxIterations", maxIterations);
                result.put("results", List.of());

                long finished = Instant.now().toEpochMilli();
                Map<String, Object> out;
                if ("loopResults".equals(outputKey)) {
                    out = Map.of(outputKey, result);
                } else {
                    out = Map.of(outputKey, result, "loopResults", result);
                }
                return WorkflowTraceEvent.builder()
                        .nodeId(node.getId())
                        .nodeType(node.getType())
                        .status("success")
                        .startedAt(started)
                        .finishedAt(finished)
                        .input(Map.of("inputs", inputs, "vars", vars))
                        .output(out)
                        .build();
            }

            // Execute the loop iterations
            List<Map<String, Object>> iterationResults = new ArrayList<>();
            
            // Determine actual iterations based on loopList and maxIterations
            // If loopList is empty, use maxIterations as the iteration count
            int actualIterations;
            boolean isListBasedLoop = listArray.size() > 0;
            
            if (isListBasedLoop) {
                // List-based loop - iterate over each item in the list
                actualIterations = Math.min(listArray.size(), maxIterations);
            } else {
                // Count-based loop - iterate for the specified number of times
                actualIterations = maxIterations;
            }

            // Determine loop body and return branch when there are exactly two successors
            List<String> loopBodySuccessors = new ArrayList<>();
            String loopReturnId = null;
            
            if (successorNodeIds.size() == 2) {
                // When there are two successors, identify which is the loop body and which is the return branch
                String successor1 = successorNodeIds.get(0);
                String successor2 = successorNodeIds.get(1);
                
                // Get the current loop node ID
                String loopNodeId = node.getId();
                
                // Create adjacency list for quick edge lookup
                Map<String, List<String>> adjacencyList = graph.getEdges().stream()
                        .collect(Collectors.groupingBy(WorkflowEdge::getFrom, 
                                Collectors.mapping(WorkflowEdge::getTo, Collectors.toList())));
                
                // Check branch 1
                List<WorkflowNode> subChain1 = findSubChain(graph, successor1);
                boolean branch1IsLoop = false;
                boolean branch1ContainsEndNode = false;
                
                // Check if branch 1 has any node pointing back to the loop node (indicating it's the loop body)
                for (WorkflowNode chainNode : subChain1) {
                    List<String> neighbors = adjacencyList.getOrDefault(chainNode.getId(), Collections.emptyList());
                    if (neighbors.contains(loopNodeId)) {
                        branch1IsLoop = true;
                        break;
                    }
                    // Check if this node is an end node
                    if ("end".equals(normalizeType(chainNode.getType()))) {
                        branch1ContainsEndNode = true;
                    }
                }
                
                // Check branch 2
                List<WorkflowNode> subChain2 = findSubChain(graph, successor2);
                boolean branch2IsLoop = false;
                boolean branch2ContainsEndNode = false;
                
                // Check if branch 2 has any node pointing back to the loop node (indicating it's the loop body)
                for (WorkflowNode chainNode : subChain2) {
                    List<String> neighbors = adjacencyList.getOrDefault(chainNode.getId(), Collections.emptyList());
                    if (neighbors.contains(loopNodeId)) {
                        branch2IsLoop = true;
                        break;
                    }
                    // Check if this node is an end node
                    if ("end".equals(normalizeType(chainNode.getType()))) {
                        branch2ContainsEndNode = true;
                    }
                }
                
                // Improved loop body identification: 
                // 1. Check which branch has nodes pointing back to the loop node (that should be the loop body)
                // 2. If both or neither branch has such connections, check which branch contains the end node (that should be the return branch)
                // 3. Fallback to node count if end node is not found
                
                // Determine loop body and return branch based on back edges and end nodes
                if (branch1IsLoop && !branch2IsLoop) {
                    // Branch 1 has back edge to loop node, so it's the loop body
                    loopBodySuccessors.add(successor1);
                    loopReturnId = successor2;
                } else if (branch2IsLoop && !branch1IsLoop) {
                    // Branch 2 has back edge to loop node, so it's the loop body
                    loopBodySuccessors.add(successor2);
                    loopReturnId = successor1;
                } else if (branch1ContainsEndNode && !branch2ContainsEndNode) {
                    // Branch 1 contains end node, so it's the return branch
                    loopBodySuccessors.add(successor2);
                    loopReturnId = successor1;
                } else if (branch2ContainsEndNode && !branch1ContainsEndNode) {
                    // Branch 2 contains end node, so it's the return branch
                    loopBodySuccessors.add(successor1);
                    loopReturnId = successor2;
                } else {
                    // Fallback to node count method if no clear distinction found
                    if (subChain1.size() > subChain2.size()) {
                        loopBodySuccessors.add(successor1);
                        loopReturnId = successor2;
                    } else {
                        loopBodySuccessors.add(successor2);
                        loopReturnId = successor1;
                    }
                }
            } else {
                // For other cases, check if successor contains end node
                for (String successorId : successorNodeIds) {
                    List<WorkflowNode> subChain = findSubChain(graph, successorId);
                    for (WorkflowNode chainNode : subChain) {
                        if ("end".equals(normalizeType(chainNode.getType()))) {
                            // If chain contains end node, set end node as return branch
                            loopReturnId = chainNode.getId();
                            if (subChain.indexOf(chainNode) > 0) {
                                // If there are nodes before end node, add successor to loop body
                                loopBodySuccessors.add(successorId);
                            }
                            break;
                        }
                    }
                    if (loopReturnId == null) {
                        // No end node found, treat as loop body
                        loopBodySuccessors.add(successorId);
                    }
                }
            }

            for (int i = 0; i < actualIterations; i++) {
                // Get current item or iteration index
                Object item;
                if (isListBasedLoop) {
                    JsonNode itemNode = listArray.get(i);
                    item = objectMapper.treeToValue(itemNode, Object.class);
                } else {
                    // For count-based loop, use iteration index as the item value (start from 1)
                    item = i + 1;
                }

                // Create a copy of vars for this iteration
                Map<String, Object> iterationVars = new HashMap<>(vars);
                // Add the iteration variable to the vars
                iterationVars.put(iterationVar, item);

                // Execute each sub-node chain for this iteration
                Map<String, Object> iterationResult = new HashMap<>();
                iterationResult.put("index", i);
                iterationResult.put("item", item);
                iterationResult.put("branchResults", new ArrayList<>());

                for (String successorId : loopBodySuccessors) {
                    // Create a copy of vars for this branch in this iteration
                    Map<String, Object> branchVars = new HashMap<>(iterationVars);
                    List<WorkflowTraceEvent> branchTrace = new ArrayList<>();

                    // Execute the sub-node chain starting from the successor
                    // Pass loopReturnId as endNodeId to exclude return branch from loop body execution
                    List<WorkflowNode> subChain = findSubChain(graph, successorId, loopReturnId);
                    boolean branchSuccess = true;

                    for (WorkflowNode subNode : subChain) {
                        // Skip execution if this is the return branch node
                        if (loopReturnId != null && subNode.getId().equals(loopReturnId)) {
                            continue;
                        }
                        WorkflowTraceEvent event = executeNode(subNode, inputs, branchVars, branchTrace);
                        // Add to executed nodes to prevent duplicate execution
                        executedNodeIds.add(subNode.getId());
                        if ("error".equals(event.getStatus())) {
                            branchSuccess = false;
                            break;
                        }
                    }

                    Map<String, Object> branchResult = new HashMap<>();
                    branchResult.put("nodeId", successorId);
                    branchResult.put("success", branchSuccess);
                    branchResult.put("vars", new HashMap<>(branchVars));
                    branchResult.put("trace", branchTrace);

                    // Add branch result to iteration result
                    ((List<Map<String, Object>>) iterationResult.get("branchResults")).add(branchResult);

                    // Add branch trace events to global trace
                    if (trace != null) {
                        trace.addAll(branchTrace);
                    }

                    // If this is the last branch, merge its vars into the iteration vars
                    if (successorId.equals(successorNodeIds.get(successorNodeIds.size() - 1))) {
                        iterationVars.putAll(branchVars);
                    }
                }

                iterationResults.add(iterationResult);
            }
            
            // Execute the return branch once after all iterations are complete
            if (loopReturnId != null) {
                // Start with the original vars, then merge in the last iteration's vars if available
                Map<String, Object> returnBranchVars = new HashMap<>(vars);
                
                // If there were iterations, use the variables from the last iteration
                if (!iterationResults.isEmpty()) {
                    Map<String, Object> lastIterationResult = iterationResults.get(iterationResults.size() - 1);
                    List<Map<String, Object>> branchResults = (List<Map<String, Object>>) lastIterationResult.get("branchResults");
                    if (!branchResults.isEmpty()) {
                        Map<String, Object> lastBranchResult = branchResults.get(branchResults.size() - 1);
                        Map<String, Object> lastBranchVars = (Map<String, Object>) lastBranchResult.get("vars");
                        if (lastBranchVars != null) {
                            // Merge last iteration's variables into return branch vars
                            returnBranchVars.putAll(lastBranchVars);
                        }
                    }
                }
                
                List<WorkflowTraceEvent> returnBranchTrace = new ArrayList<>();
                
                List<WorkflowNode> returnChain = findSubChain(graph, loopReturnId);
                boolean returnBranchSuccess = true;
                
                for (WorkflowNode subNode : returnChain) {
                    WorkflowTraceEvent event = executeNode(subNode, inputs, returnBranchVars, returnBranchTrace);
                    // Add to executed nodes to prevent duplicate execution
                    executedNodeIds.add(subNode.getId());
                    if ("error".equals(event.getStatus())) {
                        returnBranchSuccess = false;
                        break;
                    }
                }
                
                // Merge the return branch vars into global vars
                vars.putAll(returnBranchVars);
                
                // Add return branch trace events to global trace
                if (trace != null) {
                    trace.addAll(returnBranchTrace);
                }
            }

            // Merge the last iteration's vars into global vars
            if (!iterationResults.isEmpty()) {
                Map<String, Object> lastIterationResult = iterationResults.get(iterationResults.size() - 1);
                List<Map<String, Object>> branchResults = (List<Map<String, Object>>) lastIterationResult.get("branchResults");
                if (!branchResults.isEmpty()) {
                    Map<String, Object> lastBranchResult = branchResults.get(branchResults.size() - 1);
                    Map<String, Object> lastBranchVars = (Map<String, Object>) lastBranchResult.get("vars");
                    if (lastBranchVars != null) {
                        vars.putAll(lastBranchVars);
                    }
                }
            }

            // Build the final result
            Map<String, Object> result = new HashMap<>();
            result.put("list", list);
            result.put("iterationVar", iterationVar);
            result.put("maxIterations", maxIterations);
            result.put("actualIterations", actualIterations);
            result.put("results", iterationResults);

            long finished = Instant.now().toEpochMilli();
            Map<String, Object> out;
            if ("loopResults".equals(outputKey)) {
                out = Map.of(outputKey, result);
            } else {
                out = Map.of(outputKey, result, "loopResults", result);
            }
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .output(out)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    private WorkflowTraceEvent runParallelNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            String calls = StringUtils.hasText(node.getParallelCalls()) ? node.getParallelCalls() : "[]";
            String outputKey = StringUtils.hasText(node.getParallelOutputKey()) ? node.getParallelOutputKey() : "parallelResults";

            // Get the current workflow graph from thread local
            WorkflowGraph graph = workflowGraphThreadLocal.get();
            if (graph == null) {
                throw new BusinessException("Workflow graph not found");
            }

            // Find all direct successor nodes of the parallel node
            List<WorkflowEdge> edges = graph.getEdges() != null ? graph.getEdges() : Collections.emptyList();
            List<String> successorNodeIds = edges.stream()
                    .filter(e -> e.getFrom().equals(node.getId()))
                    .map(WorkflowEdge::getTo)
                    .distinct()
                    .collect(Collectors.toList());

            // If there are no direct successors, return empty results
            if (successorNodeIds.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("calls", calls);
                result.put("results", List.of());
                long finished = Instant.now().toEpochMilli();
                Map<String, Object> out = Map.of(outputKey, result);
                return WorkflowTraceEvent.builder()
                        .nodeId(node.getId())
                        .nodeType(node.getType())
                        .status("success")
                        .startedAt(started)
                        .finishedAt(finished)
                        .input(Map.of("inputs", inputs, "vars", vars))
                        .output(out)
                        .build();
            }

            // Execute each sub-node chain in parallel
            List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();
            for (String successorId : successorNodeIds) {
                CompletableFuture<Map<String, Object>> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        // Create a copy of vars for each parallel branch
                        Map<String, Object> branchVars = new HashMap<>(vars);
                        
                        // Execute the sub-node chain starting from the successor
                        List<WorkflowNode> subChain = findSubChain(graph, successorId);
                        List<WorkflowTraceEvent> branchTrace = new ArrayList<>();
                        Map<String, Object> branchResult = new HashMap<>();
                        
                        for (WorkflowNode subNode : subChain) {
                            WorkflowTraceEvent event = executeNode(subNode, inputs, branchVars, branchTrace);
                            if ("error".equals(event.getStatus())) {
                                throw new BusinessException("Branch execution failed: " + event.getError());
                            }
                        }
                        
                        // Return the final state of vars from this branch
                        branchResult.put("nodeId", successorId);
                        branchResult.put("vars", new HashMap<>(branchVars));
                        branchResult.put("trace", branchTrace);
                        return branchResult;
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                });
                futures.add(future);
            }

            // Wait for all parallel branches to complete and collect results
            List<Map<String, Object>> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            // Merge the results from all branches into global vars
            // Use the last branch's vars as the base, then merge others
            for (int i = results.size() - 1; i >= 0; i--) {
                Map<String, Object> branchResult = results.get(i);
                Map<String, Object> branchVars = (Map<String, Object>) branchResult.get("vars");
                if (branchVars != null) {
                    // Merge branch vars into global vars
                    for (Map.Entry<String, Object> entry : branchVars.entrySet()) {
                        // Only merge if the variable doesn't already exist in global vars
                        // or if it's the last branch (we prioritize the last branch's results)
                        if (!vars.containsKey(entry.getKey()) || i == results.size() - 1) {
                            vars.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
            }

            // Build the final result
            Map<String, Object> result = new HashMap<>();
            result.put("calls", calls);
            result.put("results", results);

            long finished = Instant.now().toEpochMilli();
            Map<String, Object> out = Map.of(outputKey, result);
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .output(out)
                    .build();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(cause.getMessage() != null ? cause.getMessage() : cause.toString())
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    /**
     * Finds the sub-chain of nodes starting from the given node ID, following the workflow edges
     */
    private List<WorkflowNode> findSubChain(WorkflowGraph graph, String startNodeId) {
        return findSubChain(graph, startNodeId, null);
    }
    
    private List<WorkflowNode> findSubChain(WorkflowGraph graph, String startNodeId, String endNodeId) {
        Map<String, WorkflowNode> nodeMap = graph.getNodes().stream()
                .collect(Collectors.toMap(WorkflowNode::getId, n -> n));
        
        // Create adjacency list
        Map<String, List<String>> adjacencyList = graph.getEdges().stream()
                .collect(Collectors.groupingBy(WorkflowEdge::getFrom, 
                        Collectors.mapping(WorkflowEdge::getTo, Collectors.toList())));
        
        // Create reverse adjacency list for topological sorting
        Map<String, List<String>> reverseAdjacencyList = graph.getEdges().stream()
                .collect(Collectors.groupingBy(WorkflowEdge::getTo, 
                        Collectors.mapping(WorkflowEdge::getFrom, Collectors.toList())));
        
        // Calculate in-degree for each node
        Map<String, Integer> inDegree = new HashMap<>();
        for (WorkflowNode node : graph.getNodes()) {
            inDegree.put(node.getId(), 0);
        }
        
        for (WorkflowEdge edge : graph.getEdges()) {
            inDegree.put(edge.getTo(), inDegree.getOrDefault(edge.getTo(), 0) + 1);
        }
        
        // Find all nodes in the sub-chain starting from startNodeId
        Set<String> subChainNodeIds = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.offer(startNodeId);
        subChainNodeIds.add(startNodeId);
        
        // First BFS to find all nodes in the sub-chain
        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            List<String> neighbors = adjacencyList.getOrDefault(currentId, Collections.emptyList());
            for (String neighborId : neighbors) {
                if (!subChainNodeIds.contains(neighborId)) {
                    subChainNodeIds.add(neighborId);
                    queue.offer(neighborId);
                }
            }
        }
        
        // Create a subgraph containing only the nodes in the sub-chain
        List<WorkflowEdge> subEdges = graph.getEdges().stream()
                .filter(e -> subChainNodeIds.contains(e.getFrom()) && subChainNodeIds.contains(e.getTo()))
                .collect(Collectors.toList());
        
        // Calculate in-degree for the subgraph
        Map<String, Integer> subInDegree = new HashMap<>();
        for (String nodeId : subChainNodeIds) {
            subInDegree.put(nodeId, 0);
        }
        
        for (WorkflowEdge edge : subEdges) {
            subInDegree.put(edge.getTo(), subInDegree.getOrDefault(edge.getTo(), 0) + 1);
        }
        
        // Perform topological sort on the subgraph
        Deque<String> topoQueue = new ArrayDeque<>();
        // Set in-degree of start node to 0 to ensure it's processed first
        subInDegree.put(startNodeId, 0);
        
        // Add nodes with in-degree 0 to the queue
        for (Map.Entry<String, Integer> entry : subInDegree.entrySet()) {
            if (entry.getValue() == 0) {
                topoQueue.offer(entry.getKey());
            }
        }
        
        List<WorkflowNode> subChain = new ArrayList<>();
        while (!topoQueue.isEmpty()) {
            String currentId = topoQueue.poll();
            
            // Check if current node is the end node (if specified)
            if (endNodeId != null && currentId.equals(endNodeId)) {
                // Add end node to the chain but don't process its neighbors
                subChain.add(nodeMap.get(currentId));
                break;
            }
            
            subChain.add(nodeMap.get(currentId));
            
            // Get neighbors in the original adjacency list
            List<String> neighbors = adjacencyList.getOrDefault(currentId, Collections.emptyList());
            for (String neighborId : neighbors) {
                // Only process neighbors that are in the sub-chain
                if (subChainNodeIds.contains(neighborId)) {
                    // Check if neighbor is the end node
                    if (endNodeId != null && neighborId.equals(endNodeId)) {
                        // Add end node to queue but don't decrease in-degree (will be processed next)
                        topoQueue.offer(neighborId);
                        continue;
                    }
                    
                    // Decrease in-degree
                    int newDegree = subInDegree.get(neighborId) - 1;
                    subInDegree.put(neighborId, newDegree);
                    
                    // If in-degree becomes 0, add to queue
                    if (newDegree == 0) {
                        topoQueue.offer(neighborId);
                    }
                }
            }
        }
        
        return subChain;
    }
    
    /**
     * Executes a single node and returns the trace event
     */
    private WorkflowTraceEvent executeNode(WorkflowNode node, Map<String, Object> inputs, 
            Map<String, Object> vars, List<WorkflowTraceEvent> trace) {
        String normalizedType = normalizeType(node.getType());
        WorkflowTraceEvent event;
        
        switch (normalizedType) {
            case "knowledgeRetrieval":
                event = runKnowledgeRetrievalNode(node, inputs, vars);
                if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                    for (Map.Entry<?, ?> e : outMap.entrySet()) {
                        if (e.getKey() != null) {
                            vars.put(String.valueOf(e.getKey()), e.getValue());
                        }
                    }
                }
                break;
            case "textConcat":
                event = runTextConcatenationNode(node, inputs, vars);
                if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                    for (Map.Entry<?, ?> e : outMap.entrySet()) {
                        if (e.getKey() != null) {
                            vars.put(String.valueOf(e.getKey()), e.getValue());
                        }
                    }
                }
                break;
            case "variableAggregation":
                event = runVariableAggregationNode(node, inputs, vars);
                if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                    for (Map.Entry<?, ?> e : outMap.entrySet()) {
                        if (e.getKey() != null) {
                            vars.put(String.valueOf(e.getKey()), e.getValue());
                        }
                    }
                }
                break;
            case "variableUpdater":
                event = runVariableUpdaterNode(node, inputs, vars);
                if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                    for (Map.Entry<?, ?> e : outMap.entrySet()) {
                        if (e.getKey() != null) {
                            vars.put(String.valueOf(e.getKey()), e.getValue());
                        }
                    }
                }
                break;
            case "llm":
                event = runLlmNode(node, inputs, vars);
                if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                    String outputKey = StringUtils.hasText(node.getLlmOutputKey()) ? node.getLlmOutputKey() : "llmOutput";
                    Object text = outMap.get("text");
                    vars.put(outputKey, text);
                    vars.put("llmOutput", text);
                }
                break;
            case "http":
                event = runHttpNode(node, inputs, vars);
                if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                    String outputKey = StringUtils.hasText(node.getHttpOutputKey()) ? node.getHttpOutputKey() : "httpResponse";
                    vars.put(outputKey, outMap.get("httpResponse"));
                }
                break;
            case "code":
                event = runCodeNode(node, inputs, vars);
                if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                    String outputKey = StringUtils.hasText(node.getCodeOutputKey()) ? node.getCodeOutputKey() : "codeResult";
                    vars.put(outputKey, outMap.get("codeResult"));
                }
                break;
            case "extractor":
                event = runExtractorNode(node, inputs, vars);
                if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                    String outputKey = StringUtils.hasText(node.getExtractorOutputKey()) ? node.getExtractorOutputKey() : "extractedData";
                    vars.put(outputKey, outMap.get("extractedData"));
                }
                break;
            case "end":
                event = runEndNode(node, vars);
                break;
            case "textToImage":
                event = runTextToImageNode(node, inputs, vars);
                if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                    String outputKey = StringUtils.hasText(node.getTextToImageOutputKey()) ? node.getTextToImageOutputKey() : "imageUrl";
                    vars.put(outputKey, outMap.get("url"));
                }
                break;
            case "intelligentForm":
                event = runIntelligentFormNode(node, inputs, vars);
                if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                    String outputKey = StringUtils.hasText(node.getIntelligentFormOutputKey()) ? node.getIntelligentFormOutputKey() : "formData";
                    vars.put(outputKey, outMap.get("formData"));
                }
                break;
            case "manualCheck":
                event = runManualCheckNode(node, inputs, vars);
                if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                    vars.put("manualCheckResult", outMap.get("manualCheckResult"));
                }
                break;
            case "reply":
                event = runReplyNode(node, inputs, vars);
                if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                    vars.put("replyResult", outMap.get("replyResult"));
                }
                break;
            case "toolInvoke":
                event = runToolInvokeNode(node, inputs, vars);
                if ("success".equals(event.getStatus()) && event.getOutput() instanceof Map<?, ?> outMap) {
                    String outputKey = StringUtils.hasText(node.getToolOutputVar()) ? node.getToolOutputVar() : "toolOutput";
                    vars.put(outputKey, outMap.get("toolOutput"));
                }
                break;
            default:
                event = WorkflowTraceEvent.builder()
                        .nodeId(node.getId())
                        .nodeType(node.getType())
                        .status("error")
                        .startedAt(Instant.now().toEpochMilli())
                        .finishedAt(Instant.now().toEpochMilli())
                        .input(Map.of("inputs", inputs, "vars", vars))
                        .error("Unsupported node type: " + node.getType())
                        .build();
        }
        
        if (trace != null) {
            trace.add(event);
        }
        
        return event;
    }

    private WorkflowTraceEvent runManualCheckNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            String formId = node.getManualCheckFormId();
            String formVersion = StringUtils.hasText(node.getManualCheckFormVersion()) ? node.getManualCheckFormVersion() : "1.0";
            Boolean enableStageDesc = node.getManualCheckEnableStageDesc() != null ? node.getManualCheckEnableStageDesc() : false;
            String stageDesc = node.getManualCheckStageDesc();

            Map<String, Object> result = new HashMap<>();
            result.put("formId", formId);
            result.put("formVersion", formVersion);
            result.put("enableStageDesc", enableStageDesc);
            if (enableStageDesc && StringUtils.hasText(stageDesc)) {
                result.put("stageDesc", stageDesc);
            }
            result.put("status", "pending");

            long finished = Instant.now().toEpochMilli();
            Map<String, Object> out = Map.of("manualCheckResult", result);
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .output(out)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    private WorkflowTraceEvent runReplyNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            String message = StringUtils.hasText(node.getReplyMessage()) ? renderTemplate(node.getReplyMessage(), inputs, vars) : "";
            String messageType = StringUtils.hasText(node.getReplyMessageType()) ? node.getReplyMessageType() : "text";
            Boolean enableStreaming = node.getReplyEnableStreaming() != null ? node.getReplyEnableStreaming() : false;

            Map<String, Object> result = new HashMap<>();
            result.put("message", message);
            result.put("messageType", messageType);
            result.put("enableStreaming", enableStreaming);

            long finished = Instant.now().toEpochMilli();
            Map<String, Object> out = Map.of("replyResult", result);
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .output(out)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    private WorkflowTraceEvent runToolInvokeNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            String toolId = node.getToolId();
            String parameters = StringUtils.hasText(node.getToolParameters()) ? node.getToolParameters() : "[]";
            String triggerMode = StringUtils.hasText(node.getToolTriggerMode()) ? node.getToolTriggerMode() : "auto";
            String outputVar = StringUtils.hasText(node.getToolOutputVar()) ? node.getToolOutputVar() : "toolResult";

            Map<String, Object> result = new HashMap<>();
            result.put("toolId", toolId);
            result.put("parameters", parameters);
            result.put("triggerMode", triggerMode);
            result.put("output", "工具调用已配置（需要实现实际的工具调用逻辑）");

            long finished = Instant.now().toEpochMilli();
            Map<String, Object> out;
            if ("toolResult".equals(outputVar)) {
                out = Map.of(outputVar, result);
            } else {
                out = Map.of(outputVar, result, "toolResult", result);
            }
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .output(out)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    private WorkflowTraceEvent runFileExtractionNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            String extractionInput = StringUtils.hasText(node.getFileExtractionInput()) ? renderTemplate(node.getFileExtractionInput(), inputs, vars) : "";
            String extractionType = StringUtils.hasText(node.getFileExtractionType()) ? node.getFileExtractionType() : "text";
            String outputKey = StringUtils.hasText(node.getFileExtractionOutputKey()) ? node.getFileExtractionOutputKey() : "fileContent";

            Map<String, Object> result = new HashMap<>();
            result.put("input", extractionInput);
            result.put("type", extractionType);
            result.put("content", "文件提取已配置（需要实现实际的文件解析逻辑）");

            long finished = Instant.now().toEpochMilli();
            Map<String, Object> out;
            if ("fileContent".equals(outputKey)) {
                out = Map.of(outputKey, result);
            } else {
                out = Map.of(outputKey, result, "fileContent", result);
            }
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .output(out)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    private WorkflowTraceEvent runQuestionClassificationNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            String classificationInput = StringUtils.hasText(node.getQuestionClassificationInput()) ? renderTemplate(node.getQuestionClassificationInput(), inputs, vars) : "";
            String categories = StringUtils.hasText(node.getQuestionClassificationCategories()) ? node.getQuestionClassificationCategories() : "[]";
            Long agentId = node.getQuestionClassificationAgentId();
            String outputKey = StringUtils.hasText(node.getQuestionClassificationOutputKey()) ? node.getQuestionClassificationOutputKey() : "classification";

            if (agentId == null) {
                throw new BusinessException("未选择分类智能体");
            }

            AgentTestRequest request = new AgentTestRequest();
            request.setQuestion(classificationInput);
            request.setRagConfig(new RagConfig());

            AgentTestResponse response = agentService.testAgent(agentId, request);

            Map<String, Object> result = new HashMap<>();
            result.put("input", classificationInput);
            result.put("categories", categories);
            result.put("agentId", agentId);
            result.put("response", response.getReply());
            result.put("duration", response.getElapsedMs());

            long finished = Instant.now().toEpochMilli();
            Map<String, Object> out;
            if ("classification".equals(outputKey)) {
                out = Map.of(outputKey, result);
            } else {
                out = Map.of(outputKey, result, "classification", result);
            }
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .output(out)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    private WorkflowTraceEvent runQueryOptimizationNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            String optimizationInput = StringUtils.hasText(node.getQueryOptimizationInput()) ? renderTemplate(node.getQueryOptimizationInput(), inputs, vars) : "";
            Long agentId = node.getQueryOptimizationAgentId();
            String outputKey = StringUtils.hasText(node.getQueryOptimizationOutputKey()) ? node.getQueryOptimizationOutputKey() : "optimizedQuery";

            if (agentId == null) {
                throw new BusinessException("未选择查询优化智能体");
            }

            AgentTestRequest request = new AgentTestRequest();
            request.setQuestion("请优化以下查询: " + optimizationInput);
            request.setRagConfig(new RagConfig());

            AgentTestResponse response = agentService.testAgent(agentId, request);

            Map<String, Object> result = new HashMap<>();
            result.put("originalQuery", optimizationInput);
            result.put("optimizedQuery", response.getReply());
            result.put("agentId", agentId);
            result.put("duration", response.getElapsedMs());

            long finished = Instant.now().toEpochMilli();
            Map<String, Object> out;
            if ("optimizedQuery".equals(outputKey)) {
                out = Map.of(outputKey, result);
            } else {
                out = Map.of(outputKey, result, "optimizedQuery", result);
            }
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .output(out)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    private WorkflowTraceEvent runTextExtractionNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            String extractionInput = StringUtils.hasText(node.getTextExtractionInput()) ? renderTemplate(node.getTextExtractionInput(), inputs, vars) : "";
            String extractionType = StringUtils.hasText(node.getTextExtractionType()) ? node.getTextExtractionType() : "keyword";
            String outputKey = StringUtils.hasText(node.getTextExtractionOutputKey()) ? node.getTextExtractionOutputKey() : "extractionResult";

            Map<String, Object> result = new HashMap<>();
            result.put("input", extractionInput);
            result.put("type", extractionType);
            result.put("extracted", List.of());

            long finished = Instant.now().toEpochMilli();
            Map<String, Object> out;
            if ("extractionResult".equals(outputKey)) {
                out = Map.of(outputKey, result);
            } else {
                out = Map.of(outputKey, result, "extractionResult", result);
            }
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .output(out)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    private WorkflowTraceEvent runEvaluationAlgorithmsNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            String algorithmInput = StringUtils.hasText(node.getEvaluationAlgorithmsInput()) ? renderTemplate(node.getEvaluationAlgorithmsInput(), inputs, vars) : "";
            String algorithm = StringUtils.hasText(node.getEvaluationAlgorithmsAlgorithm()) ? node.getEvaluationAlgorithmsAlgorithm() : "accuracy";
            String outputKey = StringUtils.hasText(node.getEvaluationAlgorithmsOutputKey()) ? node.getEvaluationAlgorithmsOutputKey() : "evaluationResult";

            Map<String, Object> result = new HashMap<>();
            result.put("input", algorithmInput);
            result.put("algorithm", algorithm);
            result.put("score", 0.0);

            long finished = Instant.now().toEpochMilli();
            Map<String, Object> out;
            if ("evaluationResult".equals(outputKey)) {
                out = Map.of(outputKey, result);
            } else {
                out = Map.of(outputKey, result, "evaluationResult", result);
            }
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .output(out)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    private WorkflowTraceEvent runEvaluationTestSetNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            String testSetInput = StringUtils.hasText(node.getEvaluationTestSetInput()) ? renderTemplate(node.getEvaluationTestSetInput(), inputs, vars) : "";
            String testSet = StringUtils.hasText(node.getEvaluationTestSetTestSet()) ? node.getEvaluationTestSetTestSet() : "default";
            String outputKey = StringUtils.hasText(node.getEvaluationTestSetOutputKey()) ? node.getEvaluationTestSetOutputKey() : "testSetResult";

            Map<String, Object> result = new HashMap<>();
            result.put("input", testSetInput);
            result.put("testSet", testSet);
            result.put("results", List.of());

            long finished = Instant.now().toEpochMilli();
            Map<String, Object> out;
            if ("testSetResult".equals(outputKey)) {
                out = Map.of(outputKey, result);
            } else {
                out = Map.of(outputKey, result, "testSetResult", result);
            }
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .output(out)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    private WorkflowTraceEvent runEvaluationStartNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            String triggerMode = StringUtils.hasText(node.getEvaluationStartTriggerMode()) ? node.getEvaluationStartTriggerMode() : "auto";

            Map<String, Object> result = new HashMap<>();
            result.put("triggerMode", triggerMode);
            result.put("status", "started");

            long finished = Instant.now().toEpochMilli();
            Map<String, Object> out = Map.of("evaluationStartResult", result);
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .output(out)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    private WorkflowTraceEvent runEvaluationEndNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            String outputKey = StringUtils.hasText(node.getEvaluationEndOutputKey()) ? node.getEvaluationEndOutputKey() : "evaluationResult";

            Map<String, Object> result = new HashMap<>();
            result.put("status", "completed");
            result.put("summary", "评估完成");

            long finished = Instant.now().toEpochMilli();
            Map<String, Object> out;
            if ("evaluationResult".equals(outputKey)) {
                out = Map.of(outputKey, result);
            } else {
                out = Map.of(outputKey, result, "evaluationResult", result);
            }
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .output(out)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }


    

    private WorkflowTraceEvent runExtractorNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            String extractorInput = StringUtils.hasText(node.getExtractorInput()) ? renderTemplate(node.getExtractorInput(), inputs, vars) : "";
            String extractorType = StringUtils.hasText(node.getExtractorType()) ? node.getExtractorType() : "text";
            String outputKey = StringUtils.hasText(node.getExtractorOutputKey()) ? node.getExtractorOutputKey() : "extractedData";

            Map<String, Object> result = new HashMap<>();
            result.put("input", extractorInput);
            result.put("type", extractorType);
            result.put("extracted", Map.of());

            long finished = Instant.now().toEpochMilli();
            Map<String, Object> out;
            if ("extractedData".equals(outputKey)) {
                out = Map.of(outputKey, result);
            } else {
                out = Map.of(outputKey, result, "extractedData", result);
            }
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .output(out)
                    .build();
        } catch (Exception e) {
            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status("error")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("inputs", inputs, "vars", vars))
                    .error(e.getMessage() != null ? e.getMessage() : e.toString())
                    .build();
        }
    }

    private WorkflowGraph parseGraph(String graphJson) {
        if (!StringUtils.hasText(graphJson)) {
            throw new BusinessException("graph 不能为空");
        }
        try {
            return objectMapper.readValue(graphJson, WorkflowGraph.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException("graph 解析失败: " + e.getOriginalMessage());
        }
    }

    private void validateGraphBasic(String graphJson) {
        WorkflowGraph graph = parseGraph(graphJson);
        if (graph.getNodes() == null || graph.getNodes().isEmpty()) {
            throw new BusinessException("graph.nodes 不能为空");
        }

        long startCount = graph.getNodes().stream().filter(n -> {
            String t = n.getType();
            return "start".equals(t) || "startNodeStart".equals(t);
        }).count();
        long endCount = graph.getNodes().stream().filter(n -> {
            String t = n.getType();
            return "end".equals(t) || "endNodeEnd".equals(t);
        }).count();

        if (startCount != 1) {
            throw new BusinessException("必须且只能包含 1 个开始节点 (start/startNodeStart)");
        }
        if (endCount != 1) {
            throw new BusinessException("必须且只能包含 1 个结束节点 (end/endNodeEnd)");
        }

        // basic edge validation (optional)
        if (graph.getEdges() != null) {
            Set<String> nodeIds = graph.getNodes().stream().map(WorkflowNode::getId).collect(Collectors.toSet());
            for (WorkflowEdge e : graph.getEdges()) {
                if (e == null)
                    continue;
                if (StringUtils.hasText(e.getFrom()) && !nodeIds.contains(e.getFrom())) {
                    throw new BusinessException("graph.edges.from 节点不存在: " + e.getFrom());
                }
                if (StringUtils.hasText(e.getTo()) && !nodeIds.contains(e.getTo())) {
                    throw new BusinessException("graph.edges.to 节点不存在: " + e.getTo());
                }
            }
        }
    }

    private WorkflowNode findSingleNode(WorkflowGraph graph, String type) {
        List<WorkflowNode> list = graph.getNodes() == null ? Collections.emptyList()
                : graph.getNodes().stream().filter(n -> type.equals(n.getType())).collect(Collectors.toList());
        if (list.size() != 1) {
            throw new BusinessException("未找到唯一的节点: " + type);
        }
        return list.get(0);
    }

    private WorkflowNode findOptionalNode(WorkflowGraph graph, String type) {
        if (graph.getNodes() == null)
            return null;
        List<WorkflowNode> list = graph.getNodes().stream().filter(n -> type.equals(n.getType()))
                .collect(Collectors.toList());
        if (list.isEmpty())
            return null;
        if (list.size() > 1) {
            throw new BusinessException("MVP 仅支持最多 1 个 " + type + " 节点");
        }
        return list.get(0);
    }

    private String renderTemplate(String template, Map<String, Object> inputs) {
        if (!StringUtils.hasText(template))
            return "";
        JsonNode root = objectMapper.valueToTree(inputs == null ? Collections.emptyMap() : inputs);

        Matcher matcher = INPUT_VAR_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String path = matcher.group(1);
            String replacement = resolveJsonPath(root, path);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String resolveJsonPath(JsonNode root, String path) {
        if (root == null || !StringUtils.hasText(path))
            return "";
        String[] parts = path.split("\\\\.");
        JsonNode current = root;
        for (String p : parts) {
            if (current == null)
                return "";
            current = current.get(p);
        }
        if (current == null || current.isNull())
            return "";
        if (current.isTextual())
            return current.asText();
        return current.toString();
    }

    private WorkflowVO toVO(Workflow wf) {
        return WorkflowVO.builder()
                .id(wf.getId())
                .name(wf.getName())
                .description(wf.getDescription())
                .status(wf.getStatus())
                .version(wf.getVersion())
                .graph(wf.getGraph())
                .createdAt(wf.getCreatedAt())
                .updatedAt(wf.getUpdatedAt())
                .build();
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        throw new BusinessException("无法确定当前用户，请登录");
    }
}
