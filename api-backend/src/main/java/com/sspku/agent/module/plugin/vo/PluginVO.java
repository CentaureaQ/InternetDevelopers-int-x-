package com.sspku.agent.module.plugin.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 插件视图对象（返回给前端）
 */
@Data
public class PluginVO {
    
    private Long id;
    private String name;
    private String description;
    private String type;
    private String openapiSpec;
    private String config;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

