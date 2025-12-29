-- 003: 添加 agent.workflow_id 字段
-- 用于智能体关联工作流

ALTER TABLE `agent` 
ADD COLUMN `workflow_id` BIGINT NULL COMMENT '关联的工作流ID' AFTER `rag_config`,
ADD INDEX `idx_workflow_id` (`workflow_id`);

