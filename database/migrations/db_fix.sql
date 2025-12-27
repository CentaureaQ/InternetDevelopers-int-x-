-- ---------------------------------------------------------
-- 修复数据库缺失字段 rag_config 的 SQL 语句
-- 错误信息: Unknown column 'rag_config' in 'field list'
-- 原因: 代码中 Agent 实体类和 Mapper 映射文件中包含了 rag_config 字段，但数据库表 agent 中缺少该字段。
-- ---------------------------------------------------------

-- 在 agent 表中添加 rag_config 字段
-- 建议在数据库管理工具（如 phpMyAdmin, Navicat, MySQL Workbench）中执行以下语句：

ALTER TABLE agent ADD COLUMN rag_config TEXT COMMENT 'RAG配置JSON' AFTER model_config;

