package com.sspku.agent.module.plugin.core.handler;

import cn.hutool.json.JSONUtil;
import com.sspku.agent.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 条件判断插件
 */
@Component
public class LogicHandler implements BuiltinPluginHandler {

    @Override
    public String getPluginName() {
        return "条件判断插件";
    }

    @Override
    public String execute(String functionName, Map<String, Object> arguments) {
        if ("compare".equals(functionName)) {
            return compare(arguments);
        }
        throw new BusinessException("不支持的函数: " + functionName);
    }

    /**
     * 比较两个值
     * args: val1, val2, op (eq, ne, gt, lt, ge, le, contains)
     */
    private String compare(Map<String, Object> args) {
        Object val1 = args.get("val1");
        Object val2 = args.get("val2");
        String op = (String) args.get("op");
        
        if (op == null) {
            op = "eq";
        }
        
        boolean result = false;
        
        switch (op) {
            case "eq":
                result = Objects.equals(String.valueOf(val1), String.valueOf(val2));
                break;
            case "ne":
                result = !Objects.equals(String.valueOf(val1), String.valueOf(val2));
                break;
            case "contains":
                result = String.valueOf(val1).contains(String.valueOf(val2));
                break;
            case "gt": // 假设是数字
            case "lt":
            case "ge":
            case "le":
                try {
                    double d1 = Double.parseDouble(String.valueOf(val1));
                    double d2 = Double.parseDouble(String.valueOf(val2));
                    if ("gt".equals(op)) result = d1 > d2;
                    else if ("lt".equals(op)) result = d1 < d2;
                    else if ("ge".equals(op)) result = d1 >= d2;
                    else if ("le".equals(op)) result = d1 <= d2;
                } catch (Exception e) {
                    result = false;
                }
                break;
            default:
                result = false;
        }
        
        return JSONUtil.toJsonStr(Map.of("result", result));
    }
}

