package com.sspku.agent.module.plugin.core.handler;

import java.util.Map;

/**
 * 内置插件处理器接口
 */
public interface BuiltinPluginHandler {
    
    /**
     * 获取支持的插件名称（对应 Plugin.name）
     */
    String getPluginName();
    
    /**
     * 执行函数
     * @param functionName 函数名称
     * @param arguments 参数
     * @return 执行结果（JSON字符串）
     */
    String execute(String functionName, Map<String, Object> arguments);
}

