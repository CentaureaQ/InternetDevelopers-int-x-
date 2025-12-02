package com.sspku.agent.module.plugin.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sspku.agent.module.plugin.dto.FunctionDefinition;
import com.sspku.agent.module.plugin.entity.Plugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * OpenAPI 规范解析器
 * 将 OpenAPI 3.0 规范转换为 LLM Function Calling 格式
 */
@Slf4j
@Component
public class OpenAPIParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析插件的 OpenAPI 规范
     * @param plugin 插件实体
     * @return 函数定义列表
     */
    public List<FunctionDefinition> parse(Plugin plugin) {
        List<FunctionDefinition> functions = new ArrayList<>();
        
        if (plugin == null || plugin.getOpenapiSpec() == null) {
            return functions;
        }

        try {
            JsonNode root = objectMapper.readTree(plugin.getOpenapiSpec());
            
            // 1. 获取 Base URL
            String extractedBaseUrl = "";
            JsonNode servers = root.get("servers");
            if (servers != null && servers.isArray() && servers.size() > 0) {
                extractedBaseUrl = servers.get(0).get("url").asText();
            }
            final String baseUrl = extractedBaseUrl; // 确保是 final 的
            
            // 2. 遍历 Paths
            JsonNode paths = root.get("paths");
            if (paths != null && paths.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> pathIterator = paths.fields();
                while (pathIterator.hasNext()) {
                    Map.Entry<String, JsonNode> pathEntry = pathIterator.next();
                    String path = pathEntry.getKey();
                    JsonNode pathNode = pathEntry.getValue();
                    
                    // 3. 遍历 Methods (GET, POST, etc.)
                    Iterator<Map.Entry<String, JsonNode>> methodIterator = pathNode.fields();
                    while (methodIterator.hasNext()) {
                        Map.Entry<String, JsonNode> methodEntry = methodIterator.next();
                        String method = methodEntry.getKey().toUpperCase();
                        JsonNode operation = methodEntry.getValue();
                        
                        // 忽略非标准 HTTP 方法
                        if (!isValidMethod(method)) {
                            continue;
                        }
                        
                        FunctionDefinition function = parseOperation(plugin, path, method, baseUrl, operation);
                        if (function != null) {
                            functions.add(function);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("解析插件[{}] OpenAPI 失败: {}", plugin.getName(), e.getMessage());
        }
        
        return functions;
    }

    private boolean isValidMethod(String method) {
        return List.of("GET", "POST", "PUT", "DELETE", "PATCH").contains(method);
    }

    private FunctionDefinition parseOperation(Plugin plugin, String path, String method, String baseUrl, JsonNode operation) {
        FunctionDefinition function = new FunctionDefinition();
        
        // 1. 设置函数名 (operationId)
        String operationId = operation.has("operationId") ? 
                operation.get("operationId").asText() : 
                generateOperationId(method, path);
        function.setName(operationId);
        
        // 2. 设置描述
        String description = operation.has("summary") ? 
                operation.get("summary").asText() : 
                operation.has("description") ? operation.get("description").asText() : "";
        function.setDescription(description);
        
        // 3. 构建参数
        FunctionDefinition.Parameters parameters = new FunctionDefinition.Parameters();
        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();
        
        // 3.1 处理 parameters (path & query)
        if (operation.has("parameters")) {
            for (JsonNode param : operation.get("parameters")) {
                String paramName = param.get("name").asText();
                String paramIn = param.get("in").asText(); // path, query, header, cookie
                boolean isRequired = param.has("required") && param.get("required").asBoolean();
                
                // 暂时只处理 path 和 query 参数
                if (!"path".equals(paramIn) && !"query".equals(paramIn)) {
                    continue;
                }
                
                Map<String, Object> schemaMap = objectMapper.convertValue(param.get("schema"), Map.class);
                if (param.has("description")) {
                    schemaMap.put("description", param.get("description").asText());
                }
                
                properties.put(paramName, schemaMap);
                if (isRequired) {
                    required.add(paramName);
                }
            }
        }
        
        // 3.2 处理 requestBody
        if (operation.has("requestBody")) {
            JsonNode content = operation.get("requestBody").get("content");
            if (content != null && content.has("application/json")) {
                JsonNode schema = content.get("application/json").get("schema");
                if (schema != null) {
                    // 合并 properties
                    if (schema.has("properties")) {
                        Iterator<Map.Entry<String, JsonNode>> propIterator = schema.get("properties").fields();
                        while (propIterator.hasNext()) {
                            Map.Entry<String, JsonNode> prop = propIterator.next();
                            properties.put(prop.getKey(), objectMapper.convertValue(prop.getValue(), Map.class));
                        }
                    }
                    // 合并 required
                    if (schema.has("required")) {
                        for (JsonNode req : schema.get("required")) {
                            required.add(req.asText());
                        }
                    }
                }
            }
        }
        
        parameters.setProperties(properties);
        parameters.setRequired(required);
        function.setParameters(parameters);
        
        // 4. 设置元数据
        FunctionDefinition.Metadata metadata = new FunctionDefinition.Metadata();
        metadata.setPluginId(plugin.getId());
        metadata.setPluginName(plugin.getName());
        metadata.setMethod(method);
        metadata.setPath(path);
        metadata.setBaseUrl(baseUrl);
        function.setMetadata(metadata);
        
        return function;
    }

    private String generateOperationId(String method, String path) {
        // 将 /api/v1/user 转换为 get_api_v1_user
        return method.toLowerCase() + path.replace("/", "_").replace("{", "").replace("}", "");
    }
}
