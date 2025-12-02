package com.sspku.agent.module.plugin.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sspku.agent.common.exception.BusinessException;
import com.sspku.agent.module.plugin.core.handler.BuiltinPluginHandler;
import com.sspku.agent.module.plugin.dto.FunctionDefinition;
import com.sspku.agent.module.plugin.entity.Plugin;
import com.sspku.agent.module.plugin.service.IPluginService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 插件执行器实现
 */
@Slf4j
@Component
public class PluginExecutorImpl implements PluginExecutor {

    @Resource
    private IPluginService pluginService;

    @Resource
    private OpenAPIParser openAPIParser;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private List<BuiltinPluginHandler> builtinHandlers;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Handler 映射缓存
    private Map<String, BuiltinPluginHandler> handlerMap;

    @Override
    public String execute(Long pluginId, String functionName, Map<String, Object> arguments) {
        log.info("准备执行插件[{}], 函数[{}], 参数: {}", pluginId, functionName, arguments);

        // 1. 获取插件信息
        var pluginVO = pluginService.getPluginById(pluginId);
        
        if (!"enabled".equals(pluginVO.getStatus())) {
            throw new BusinessException("插件未启用");
        }

        // 2. 判断是内置插件还是外部插件
        if ("builtin".equals(pluginVO.getType())) {
            return executeBuiltin(pluginVO.getName(), functionName, arguments);
        }

        // 3. 外部插件：解析 OpenAPI，找到对应函数
        Plugin plugin = new Plugin();
        plugin.setId(pluginVO.getId());
        plugin.setName(pluginVO.getName());
        plugin.setOpenapiSpec(pluginVO.getOpenapiSpec());
        plugin.setConfig(pluginVO.getConfig());

        List<FunctionDefinition> functions = openAPIParser.parse(plugin);
        FunctionDefinition targetFunction = functions.stream()
                .filter(f -> f.getName().equals(functionName))
                .findFirst()
                .orElseThrow(() -> new BusinessException("未找到函数: " + functionName));

        // 4. 准备请求参数
        FunctionDefinition.Metadata metadata = targetFunction.getMetadata();
        String url = metadata.getBaseUrl() + metadata.getPath();
        String method = metadata.getMethod().toUpperCase();
        
        // 5. 构建请求
        return doExecute(url, method, arguments, plugin.getConfig());
    }

    /**
     * 执行内置插件
     */
    private String executeBuiltin(String pluginName, String functionName, Map<String, Object> arguments) {
        // 初始化 handler 映射（懒加载）
        if (handlerMap == null) {
            handlerMap = builtinHandlers.stream()
                    .collect(Collectors.toMap(BuiltinPluginHandler::getPluginName, h -> h));
        }

        BuiltinPluginHandler handler = handlerMap.get(pluginName);
        if (handler == null) {
            throw new BusinessException("未找到内置插件处理器: " + pluginName);
        }

        log.info("执行内置插件[{}], 函数[{}]", pluginName, functionName);
        return handler.execute(functionName, arguments);
    }

    private String doExecute(String urlTemplate, String method, Map<String, Object> arguments, String configJson) {
        try {
            // 4.1 处理路径参数 (e.g. /users/{id})
            Map<String, Object> uriVariables = new HashMap<>();
            // 4.2 处理查询参数 (e.g. ?city=Beijing)
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(urlTemplate);
            // 4.3 处理请求体
            Map<String, Object> requestBody = new HashMap<>();

            // 简单策略：如果是 GET/DELETE，所有参数默认作为 query param
            // 如果是 POST/PUT，参数如果没有在 path 中，则作为 body
            // 更严谨的做法是根据 FunctionDefinition.parameters 来区分
            
            // 这里为了简化，我们假设 arguments 包含了所有参数
            // 我们遍历 urlTemplate 中的 {param}，如果在 arguments 中有，则作为 path param，否则...
            
            for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                if (urlTemplate.contains("{" + key + "}")) {
                    uriVariables.put(key, value);
                } else if ("GET".equals(method) || "DELETE".equals(method)) {
                    builder.queryParam(key, value);
                } else {
                    requestBody.put(key, value);
                }
            }

            String finalUrl = builder.buildAndExpand(uriVariables).toUriString();
            log.info("发送请求: {} {}", method, finalUrl);

            // 4.4 设置 Headers (如 API Key)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 解析 config，添加认证头
            if (configJson != null && !configJson.isEmpty()) {
                try {
                    Map<String, String> config = objectMapper.readValue(configJson, Map.class);
                    // 假设 config 中配置了 apiKey，我们需要知道它放在哪里 (header/query)
                    // 这里为了演示，假设如果是 apiKey 字段，就放在 Header: Authorization: Bearer xxx
                    if (config.containsKey("apiKey")) {
                        headers.set("Authorization", "Bearer " + config.get("apiKey"));
                    }
                    // 也可以遍历所有 config 添加到 header
                    config.forEach((k, v) -> {
                        if (!"apiKey".equals(k) && !"baseUrl".equals(k)) {
                            headers.set(k, v);
                        }
                    });
                } catch (Exception e) {
                    log.warn("解析插件配置失败", e);
                }
            }

            HttpEntity<?> entity;
            if ("GET".equals(method) || "DELETE".equals(method)) {
                entity = new HttpEntity<>(headers);
            } else {
                entity = new HttpEntity<>(requestBody, headers);
            }

            // 5. 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    finalUrl,
                    HttpMethod.valueOf(method),
                    entity,
                    String.class
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("执行插件请求失败", e);
            throw new BusinessException("插件执行失败: " + e.getMessage());
        }
    }
}

