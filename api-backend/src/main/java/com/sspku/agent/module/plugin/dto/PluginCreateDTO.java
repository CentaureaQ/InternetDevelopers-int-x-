package com.sspku.agent.module.plugin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 插件创建请求对象
 */
@Data
public class PluginCreateDTO {
    
    @NotBlank(message = "插件名称不能为空")
    private String name;
    
    private String description;
    
    @NotBlank(message = "OpenAPI规范不能为空")
    private String openapiSpec;
    
    private String config;
}

