package com.sspku.agent.module.plugin.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * LLM Function Calling 定义
 * 对应 OpenAI/Deepseek 的 function 格式
 */
@Data
public class FunctionDefinition {
    
    /**
     * 函数名称（对应 operationId）
     */
    private String name;
    
    /**
     * 函数描述（对应 summary/description）
     */
    private String description;
    
    /**
     * 参数定义（JSON Schema）
     */
    private Parameters parameters;
    
    /**
     * 扩展元数据（用于执行时使用，不传给LLM）
     */
    private Metadata metadata;

    @Data
    public static class Parameters {
        private String type = "object";
        private Map<String, Object> properties;
        private List<String> required;
    }
    
    @Data
    public static class Metadata {
        private Long pluginId;
        private String pluginName;
        private String method; // GET, POST, etc.
        private String path;   // /api/v1/weather
        private String baseUrl; // https://api.example.com
    }
}

