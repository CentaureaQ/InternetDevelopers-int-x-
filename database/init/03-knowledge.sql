-- 知识库表
DROP TABLE IF EXISTS `knowledge_chunk`;
DROP TABLE IF EXISTS `document`;
DROP TABLE IF EXISTS `knowledge_base`;

CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `uuid` VARCHAR(64) NOT NULL COMMENT 'UUID',
    `name` VARCHAR(100) NOT NULL COMMENT '知识库名称',
    `description` VARCHAR(500) COMMENT '描述',
    `icon` VARCHAR(255) COMMENT '图标',
    `level` VARCHAR(20) NOT NULL DEFAULT 'personal' COMMENT '级别: system/school/course/agent/personal',
    `parent_kb_id` BIGINT COMMENT '父知识库ID',
    `vector_db_type` VARCHAR(50) DEFAULT 'milvus' COMMENT '向量库类型',
    `embedding_model_id` VARCHAR(100) COMMENT 'Embedding模型ID',
    `chunk_size` INT DEFAULT 512 COMMENT '分块大小',
    `chunk_overlap` INT DEFAULT 50 COMMENT '分块重叠',
    `retrieval_config` JSON COMMENT '检索配置',
    `access_level` VARCHAR(20) DEFAULT 'private' COMMENT '访问级别: public/protected/private',
    `owner_id` BIGINT COMMENT '创建者ID',
    `document_count` INT DEFAULT 0 COMMENT '文档数量',
    `chunk_count` INT DEFAULT 0 COMMENT '分块数量',
    `total_size` BIGINT DEFAULT 0 COMMENT '总文件大小(字节)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_uuid` (`uuid`),
    INDEX `idx_level` (`level`),
    INDEX `idx_owner` (`owner_id`),
    INDEX `idx_access` (`access_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- 文档表
CREATE TABLE IF NOT EXISTS `document` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `uuid` VARCHAR(64) NOT NULL COMMENT 'UUID',
    `name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    `file_size` BIGINT NOT NULL COMMENT '文件大小(字节)',
    `file_type` VARCHAR(20) NOT NULL COMMENT '文件类型(txt/md)',
    `knowledge_base_id` BIGINT NOT NULL COMMENT '所属知识库ID',
    `status` VARCHAR(20) DEFAULT 'uploading' COMMENT '状态: uploading/processing/processed/failed',
    `chunk_count` INT DEFAULT 0 COMMENT '分块数量',
    `error_message` TEXT COMMENT '错误信息',
    `processed_at` DATETIME COMMENT '处理完成时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_uuid` (`uuid`),
    INDEX `idx_kb_id` (`knowledge_base_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';

-- 文档分块表
CREATE TABLE IF NOT EXISTS `knowledge_chunk` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `document_id` BIGINT NOT NULL COMMENT '所属文档ID',
    `kb_id` BIGINT NOT NULL COMMENT '所属知识库ID',
    `chunk_index` INT NOT NULL COMMENT '分块索引',
    `content` LONGTEXT COMMENT '分块内容',
    `token_count` INT COMMENT 'Token数量',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_doc_id` (`document_id`),
    INDEX `idx_kb_id` (`kb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档分块表';
