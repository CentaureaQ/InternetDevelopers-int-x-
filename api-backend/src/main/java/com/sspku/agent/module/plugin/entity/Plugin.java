package com.sspku.agent.module.plugin.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 插件实体类
 * 用于存储和管理智能体插件（遵循 OpenAPI 3.0 规范）
 */
@Data
public class Plugin {
    /**
     * 插件ID
     */
    private Long id;

    /**
     * 插件名称（唯一）
     */
    private String name;

    /**
     * 插件描述
     */
    private String description;

    /**
     * 插件类型
     * builtin - 内置插件（系统预定义）
     * custom - 自定义插件（用户创建）
     */
    private String type;

    /**
     * OpenAPI 3.0 规范（JSON格式字符串）
     * 包含插件的完整API定义
     */
    private String openapiSpec;

    /**
     * 插件配置（JSON格式字符串）
     * 存储API密钥、服务器地址等配置信息
     * 示例：{"apiKey": "xxx", "baseUrl": "https://api.example.com"}
     */
    private String config;

    /**
     * 插件状态
     * enabled - 已启用（可被智能体使用）
     * disabled - 已禁用（不可使用）
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

}
