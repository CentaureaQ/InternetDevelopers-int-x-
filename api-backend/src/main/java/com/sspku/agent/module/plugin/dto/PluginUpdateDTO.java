package com.sspku.agent.module.plugin.dto;

import lombok.Data;

/**
 * 插件更新请求对象
 */
@Data
public class PluginUpdateDTO {
    
    private String name;
    
    private String description;
    
    private String openapiSpec;
    
    private String config;
    
    private String status;
}

