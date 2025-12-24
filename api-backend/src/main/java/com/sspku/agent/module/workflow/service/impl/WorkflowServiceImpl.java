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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
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
    public WorkflowDebugResponse debugWorkflow(Long id, WorkflowDebugRequest request) {
        Long ownerId = currentUserId();
        Workflow wf = workflowMapper.selectByIdAndOwner(id, ownerId);
        if (wf == null) {
            throw new BusinessException("工作流不存在");
        }

        WorkflowGraph graph = parseGraph(wf.getGraph());
        validateGraphBasic(wf.getGraph());

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

        for (WorkflowNode node : executionOrder) {
            String normalizedType = normalizeType(node.getType());
            WorkflowTraceEvent event;

            switch (normalizedType) {
                case "start":
                    event = runStartNode(node, inputs);
                    nodeOutputs.put(node.getId(), event.getOutput());
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
                    break;
                case "llm":
                    event = runLlmNode(node, inputs, vars);
                    if ("success".equals(event.getStatus())) {
                        String outputKey = StringUtils.hasText(node.getLlmOutputKey()) ? node.getLlmOutputKey()
                                : "llmOutput";
                        vars.put(outputKey, event.getOutput());
                        // keep a common alias
                        vars.put("llmOutput", event.getOutput());
                    }
                    nodeOutputs.put(node.getId(), event.getOutput());
                    break;
                case "end":
                    event = runEndNode(node, vars);
                    nodeOutputs.put(node.getId(), event.getOutput());
                    finalOutput = event.getOutput();
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
            String model = StringUtils.hasText(llm.getModel()) ? llm.getModel() : "qwen-max-latest";
            String template = StringUtils.hasText(llm.getPrompt()) ? llm.getPrompt() : "请回答：{{inputs.query}}";
            String promptText = renderTemplate(template, inputs, vars);

            Prompt prompt = new Prompt(List.of(new UserMessage(promptText)),
                    org.springframework.ai.model.tool.ToolCallingChatOptions.builder()
                            .model(model)
                            .temperature(0.7)
                            .topP(0.9)
                            .build());

            ChatClient chatClient = ChatClient.builder(chatModel).build();
            ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
            String reply = response.getResult().getOutput().getText();

            long finished = Instant.now().toEpochMilli();
            return WorkflowTraceEvent.builder()
                    .nodeId(llm.getId())
                    .nodeType(llm.getType())
                    .status("success")
                    .startedAt(started)
                    .finishedAt(finished)
                    .input(Map.of("model", model, "prompt", promptText))
                    .output(reply)
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
                    .error(e.getMessage())
                    .build();
        }
    }

    private WorkflowTraceEvent runKnowledgeRetrievalNode(WorkflowNode node, Map<String, Object> inputs,
            Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        try {
            String queryTemplate = StringUtils.hasText(node.getQueryTemplate()) ? node.getQueryTemplate()
                    : "{{inputs.query}}";
            String query = renderTemplate(queryTemplate, inputs, vars);

            String agentIdKey = StringUtils.hasText(node.getAgentIdKey()) ? node.getAgentIdKey() : "agentId";
            Object agentIdRaw = inputs.get(agentIdKey);
            Long agentId = null;
            if (agentIdRaw instanceof Number n) {
                agentId = n.longValue();
            } else if (agentIdRaw != null) {
                try {
                    agentId = Long.parseLong(String.valueOf(agentIdRaw));
                } catch (Exception ignore) {
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
                    .error(e.getMessage())
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
                    .error(e.getMessage())
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
                    .error(e.getMessage())
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
                    .error(e.getMessage())
                    .build();
        }
    }

    private WorkflowTraceEvent runEndNode(WorkflowNode end, Map<String, Object> vars) {
        long started = Instant.now().toEpochMilli();
        long finished = started;
        String outputKey = StringUtils.hasText(end.getOutputKey()) ? end.getOutputKey() : "answer";
        Object out = vars.get(outputKey);
        Map<String, Object> output = Map.of(outputKey, out);
        return WorkflowTraceEvent.builder()
                .nodeId(end.getId())
                .nodeType(end.getType())
                .status("success")
                .startedAt(started)
                .finishedAt(finished)
                .input(Map.of("outputKey", outputKey))
                .output(output)
                .build();
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
