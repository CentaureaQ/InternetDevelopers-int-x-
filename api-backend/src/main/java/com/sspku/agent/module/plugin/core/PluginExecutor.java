package com.sspku.agent.module.plugin.core;

import java.util.Map;

/**
 * 插件执行器接口
 */
public interface PluginExecutor {

    /**
     * 执行插件函数
     * @param pluginId 插件ID
     * @param functionName 函数名称（operationId）
     * @param arguments 参数（Key-Value）
     * @return 执行结果（JSON字符串）
     */
    String execute(Long pluginId, String functionName, Map<String, Object> arguments);
}

