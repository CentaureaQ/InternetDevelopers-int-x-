-- 智能体与知识库关联表
-- 对应用户故事: US-013 智能体对话
-- 作用: 建立智能体与知识库的多对多关系

-- 添加agent_knowledge_base_id字段到agent表（临时方案，最终使用关联表）
ALTER TABLE `agent` ADD COLUMN `knowledge_base_id` BIGINT COMMENT '关联知识库ID' AFTER `rag_config`;

-- 创建智能体与知识库关联表（推荐方案）
CREATE TABLE IF NOT EXISTS `agent_knowledge_rel` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `agent_id` BIGINT NOT NULL COMMENT '智能体ID',
    `knowledge_base_id` BIGINT NOT NULL COMMENT '知识库ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_agent_kb` (`agent_id`, `knowledge_base_id`),
    INDEX `idx_agent_id` (`agent_id`),
    INDEX `idx_knowledge_base_id` (`knowledge_base_id`),
    FOREIGN KEY `fk_agent_id` (`agent_id`) REFERENCES `agent`(`id`) ON DELETE CASCADE,
    FOREIGN KEY `fk_knowledge_base_id` (`knowledge_base_id`) REFERENCES `knowledge_base`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体知识库关联表';

-- 插入一些示例数据（可选）
-- INSERT INTO `agent_knowledge_rel` (`agent_id`, `knowledge_base_id`) VALUES
-- (1, 1),
-- (1, 2);