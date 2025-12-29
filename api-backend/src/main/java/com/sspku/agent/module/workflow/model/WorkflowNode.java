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
    private Long llmAgentId;
    private String systemPrompt;  // 系统提示词
    private String prompt;         // 用户提示词
    private String llmOutputKey;
    private java.util.List<Long> pluginIds;
    // direct model selection (bypassing agent)
    private String modelProvider;
    private String modelName;
    private Double modelTemperature;
    private Integer modelMaxTokens;
    private Double modelTopP;

    // knowledgeRetrieval
    private String queryTemplate;
    private Long agentId;
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

    // textToImage
    private String textToImagePrompt;
    private Long textToImageAgentId;
    private String textToImageOutputKey;
    // direct model selection for textToImage (bypassing agent)
    private String textToImageModelProvider;
    private String textToImageModelName;
    private Integer textToImageWidth;
    private Integer textToImageHeight;

    // intelligentForm
    private String intelligentFormFormId;
    private String intelligentFormFormVersion;
    private Boolean intelligentFormEnableStageDesc;
    private String intelligentFormStageDesc;
    private String intelligentFormOutputKey;

    // condition
    private Map<String, Object> conditionParams;

    // http
    private String httpUrl;
    private String httpMethod;
    private String httpHeaders;
    private String httpBody;
    private String httpOutputKey;

    // code
    private String codeLanguage;
    private String codeScript;
    private String codeOutputKey;

    // loop
    private String loopList;
    private String loopIterationVar;
    private Integer loopMaxIterations;
    private String loopOutputKey;

    // parallel
    private String parallelCalls;
    private String parallelOutputKey;

    // manualCheck
    private String manualCheckFormId;
    private String manualCheckFormVersion;
    private Boolean manualCheckEnableStageDesc;
    private String manualCheckStageDesc;

    // reply
    private String replyMessage;
    private String replyMessageType;
    private Boolean replyEnableStreaming;

    // toolInvoke
    private String toolId;
    private String toolParameters;
    private String toolTriggerMode;
    private String toolOutputVar;

    // fileExtraction
    private String fileExtractionInput;
    private String fileExtractionType;
    private String fileExtractionOutputKey;

    // questionClassification
    private String questionClassificationInput;
    private String questionClassificationCategories;
    private Long questionClassificationAgentId;
    private String questionClassificationOutputKey;

    // queryOptimization
    private String queryOptimizationInput;
    private Long queryOptimizationAgentId;
    private String queryOptimizationOutputKey;

    // textExtraction
    private String textExtractionInput;
    private String textExtractionType;
    private String textExtractionOutputKey;

    // evaluationAlgorithms
    private String evaluationAlgorithmsInput;
    private String evaluationAlgorithmsAlgorithm;
    private String evaluationAlgorithmsOutputKey;

    // evaluationTestSet
    private String evaluationTestSetInput;
    private String evaluationTestSetTestSet;
    private String evaluationTestSetOutputKey;

    // evaluationStart
    private String evaluationStartTriggerMode;

    // evaluationEnd
    private String evaluationEndOutputKey;

    // huggingFace
    private Long huggingFaceAgentId;
    private String huggingFaceInput;
    private String huggingFaceOutputKey;

    // extractor
    private String extractorInput;
    private String extractorType;
    private String extractorOutputKey;
}
