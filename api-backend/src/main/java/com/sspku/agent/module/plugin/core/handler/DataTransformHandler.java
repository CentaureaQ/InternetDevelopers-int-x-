package com.sspku.agent.module.plugin.core.handler;

import cn.hutool.json.JSONUtil;
import com.sspku.agent.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 数据转换插件
 */
@Component
public class DataTransformHandler implements BuiltinPluginHandler {

    @Override
    public String getPluginName() {
        return "数据转换插件";
    }

    @Override
    public String execute(String functionName, Map<String, Object> arguments) {
        if ("extract".equals(functionName)) {
            return extract(arguments);
        } else if ("format".equals(functionName)) {
            return format(arguments);
        }
        throw new BusinessException("不支持的函数: " + functionName);
    }

    /**
     * 提取 JSON 数据
     * args: source(json string/object), path(json path)
     */
    private String extract(Map<String, Object> args) {
        Object source = args.get("source");
        String path = (String) args.get("path");
        
        if (source == null || path == null) {
            throw new BusinessException("参数 source 和 path 不能为空");
        }
        
        // 如果 source 是字符串，尝试解析为 JSON
        Object jsonSource = source;
        if (source instanceof String) {
            try {
                jsonSource = JSONUtil.parse((String) source);
            } catch (Exception e) {
                // 不是 JSON 字符串，直接作为对象处理
            }
        }
        
        Object result = JSONUtil.getByPath(JSONUtil.parse(jsonSource), path);
        return JSONUtil.toJsonStr(result);
    }

    /**
     * 格式化字符串 (简单模版替换)
     * args: template("Hello {name}"), data({"name": "World"})
     */
    private String format(Map<String, Object> args) {
        String template = (String) args.get("template");
        Map<String, Object> data = (Map<String, Object>) args.get("data");
        
        if (template == null) {
            return "";
        }
        
        if (data == null) {
            return template;
        }
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            template = template.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        
        // 返回 JSON 格式的字符串，以便统一
        return JSONUtil.toJsonStr(Map.of("result", template));
    }
}

