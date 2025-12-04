# 后端知识库模块开发 - 第一阶段修改记录

## 1. 数据库变更
创建了新的初始化脚本 `database/init/03-knowledge.sql`，新增了以下表结构：
- **knowledge_base**: 存储知识库基础配置信息（名称、描述、向量库类型、分块策略等）。
- **document**: 存储文档元数据（UUID、文件路径、状态、分块统计等），支持状态流转（uploading -> processing -> processed/failed）。

## 2. 后端代码新增

### 实体类 (Entity)
位置: `api-backend/src/main/java/com/sspku/agent/module/knowledge/entity/`
- **KnowledgeBase.java**: 知识库实体，对应数据库 `knowledge_base` 表。
- **KnowledgeDocument.java**: 文档实体，对应数据库 `document` 表，包含文件元数据和处理状态。

### 数据访问层 (Mapper)
位置: `api-backend/src/main/java/com/sspku/agent/module/knowledge/mapper/`
- **KnowledgeBaseMapper.java**: 提供知识库的查询接口。
- **KnowledgeDocumentMapper.java**: 提供文档的增删改查接口，支持按知识库UUID筛选文档列表。

位置: `api-backend/src/main/resources/mapper/`
- **KnowledgeBaseMapper.xml**: MyBatis XML 映射文件。
- **KnowledgeDocumentMapper.xml**: MyBatis XML 映射文件，包含具体的 SQL 实现。

### 业务逻辑层 (Service)
位置: `api-backend/src/main/java/com/sspku/agent/module/knowledge/service/`
- **KnowledgeDocumentService.java**: 服务接口定义。
- **impl/KnowledgeDocumentServiceImpl.java**: 服务实现类。
    - **文件上传**: 实现了文件格式校验（仅限 TXT/Markdown）、大小限制（10MB）和本地存储。
    - **异步处理**: 实现了 `processDocumentAsync` 方法（目前为模拟），用于后续接入文档解析和向量化。
    - **文档管理**: 实现了文档列表查询、详情获取和删除功能。

### 控制层 (Controller)
位置: `api-backend/src/main/java/com/sspku/agent/module/knowledge/controller/`
- **KnowledgeDocumentController.java**: 暴露 RESTful API 接口。
    - `GET /api/v1/knowledge-bases/{kbUuid}/documents`: 获取文档列表。
    - `POST /api/v1/knowledge-bases/{kbUuid}/documents`: 上传文档。
    - `GET /api/v1/documents/{uuid}`: 获取文档详情。
    - `DELETE /api/v1/documents/{uuid}`: 删除文档。

## 3. 功能特性
- **文件验证**: 严格限制文件格式为 `.txt`, `.md`, `.markdown`，最大支持 10MB。
- **状态管理**: 完整的文档处理状态流转机制。
- **异步架构**: 采用异步线程处理耗时的文档解析任务，避免阻塞 HTTP 请求。
