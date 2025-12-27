package com.sspku.agent.module.agent.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sspku.agent.module.plugin.core.OpenAPIParser;
import com.sspku.agent.module.plugin.core.PluginExecutor;
import com.sspku.agent.module.plugin.dto.FunctionDefinition;
import com.sspku.agent.module.plugin.entity.Plugin;
import com.sspku.agent.module.plugin.service.IPluginService;
import com.sspku.agent.module.plugin.vo.PluginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 插件工具工厂
 * 将 OpenAPI 插件转换为 Spring AI ToolCallback
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PluginToolFactory {

    private final IPluginService pluginService;
    private final OpenAPIParser openAPIParser;
    private final PluginExecutor pluginExecutor;
    private final ObjectMapper objectMapper;

    /**
     * 根据插件ID列表创建 ToolCallback 列表
     */
    public List<ToolCallback> createToolCallbacks(List<Long> pluginIds) {
        List<ToolCallback> toolCallbacks = new ArrayList<>();
        if (pluginIds == null || pluginIds.isEmpty()) {
            return toolCallbacks;
        }

        for (Long pluginId : pluginIds) {
            try {
                // 1. 获取插件信息
                PluginVO pluginVO = pluginService.getPluginById(pluginId);
                if (!"enabled".equals(pluginVO.getStatus())) {
                    continue;
                }

                // 2. 解析函数定义
                Plugin plugin = new Plugin();
                plugin.setId(pluginVO.getId());
                plugin.setName(pluginVO.getName());
                plugin.setOpenapiSpec(pluginVO.getOpenapiSpec());

                List<FunctionDefinition> functions = openAPIParser.parse(plugin);

                // 3. 为每个函数创建 ToolCallback
                for (FunctionDefinition funcDef : functions) {
                    toolCallbacks.add(createToolCallback(pluginId, funcDef));
                }

            } catch (Exception e) {
                log.error("创建插件工具失败: pluginId={}", pluginId, e);
            }
        }
        return toolCallbacks;
    }

    private ToolCallback createToolCallback(Long pluginId, FunctionDefinition funcDef) {
        // 构造 JSON Schema
        String inputSchema = "{}";
        try {
            inputSchema = objectMapper.writeValueAsString(funcDef.getParameters());
        } catch (JsonProcessingException e) {
            log.error("序列化参数Schema失败", e);
        }

        // 创建执行函数
        // Spring AI 会将 LLM 的 JSON 参数反序列化为 Map
        Function<Map<String, Object>, String> toolFunction = args -> {
            log.info("调用插件工具: pluginId={}, function={}, args={}", pluginId, funcDef.getName(), args);
            return pluginExecutor.execute(pluginId, funcDef.getName(), args);
        };

        return FunctionToolCallback
                .builder(funcDef.getName(), toolFunction)
                .description(funcDef.getDescription())
                .inputType(Map.class) // 使用 Map 接收参数
                .inputSchema(inputSchema) // 显式提供 Schema
                .build();
    }
}
