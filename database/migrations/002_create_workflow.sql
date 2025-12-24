-- Create workflow table for MVP workflow editor/debug
-- Supports minimal node types: start/llm/end, graph stored as JSON string

CREATE TABLE IF NOT EXISTS `workflow` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `owner_user_id` BIGINT NOT NULL COMMENT '拥有者用户ID',
  `name` VARCHAR(100) NOT NULL COMMENT '工作流名称',
  `description` VARCHAR(500) NULL COMMENT '工作流描述',
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态: draft/published',
  `version` VARCHAR(20) NOT NULL DEFAULT '1.0' COMMENT '版本(字符串)',
  `graph` LONGTEXT NOT NULL COMMENT '工作流图(JSON字符串)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_workflow_owner` (`owner_user_id`),
  INDEX `idx_workflow_status` (`status`),
  INDEX `idx_workflow_updated` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流表';
