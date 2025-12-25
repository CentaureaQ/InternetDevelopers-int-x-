package com.sspku.agent.module.workflow.model;

import lombok.Data;

import java.util.Map;

/**
 * Graph node aligned with web-vue workflow editor.
 *
 * NOTE: This model intentionally supports a small subset of the doc-style node
 * types
 * (e.g. startNodeStart/llmNodeState/endNodeEnd) with minimal config needed for
 * debug.
 */
@Data
public class WorkflowNode {
    private String id;
    /**
     * Node type.
     * Legacy: start | llm | end
     * Doc-style: startNodeStart | knowledgeRetrievalNodeState |
     * textConcatenationNodeState | variableAggregationNodeState |
     * variableUpdaterNodeState | llmNodeState | endNodeEnd
     */
    private String type;
    private Double x;
    private Double y;

    /**
     * Optional raw config bag for future compatibility.
     * Not required by current execution but allows graph JSON to carry extra
     * fields.
     */
    private Map<String, Object> flowMeta;

    // llm (legacy + doc-style)
    private String model;
    private String prompt;
    private String llmOutputKey;
    private java.util.List<Long> pluginIds;

    // knowledgeRetrieval
    private String queryTemplate;
    private String agentIdKey;
    private String knowledgeOutputKey;

    // textConcatenation / variableAggregation
    private String partsText;
    private String separator;
    private String textOutputKey;

    // variableUpdater
    private String targetKey;
    private String valueTemplate;

    // end (legacy + doc-style)
    private String outputKey;
}
