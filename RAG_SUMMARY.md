# RAG 模块开发总结

## 1. 功能概述
实现了基于 RAG (Retrieval-Augmented Generation) 的知识库增强检索功能，包括文档切分、向量化存储、向量检索和提示词构建。

## 2. 核心组件

### 2.1 文档处理 (Chunking)
- **位置**: `api-backend/src/main/java/com/sspku/agent/module/knowledge/component/chunker/`
- **功能**: 将上传的文档切分为较小的文本块，以便于向量化和检索。
- **策略**:
    - `TextChunker`: 递归字符切分，优先使用段落和句子分隔符。
    - `MarkdownChunker`: 针对 Markdown 文档优化，保留标题结构。
- **配置**: 支持自定义 `chunkSize` (默认 800) 和 `chunkOverlap` (默认 50)。

### 2.2 向量化 (Embedding)
- **位置**: `api-backend/src/main/java/com/sspku/agent/module/knowledge/service/impl/TongyiEmbeddingServiceImpl.java`
- **模型**: 通义千问 `text-embedding-v1`。
- **特性**:
    - 批量处理 (Batch Size: 10)。
    - 自动重试机制 (Max Retries: 3)。
    - API 限流保护 (200ms 延迟)。
    - 真实 API 调用 (已移除 Mock)。

### 2.3 向量存储 (Vector Store)
- **位置**: `api-backend/src/main/java/com/sspku/agent/module/knowledge/service/impl/MilvusVectorStorageServiceImpl.java`
- **数据库**: Milvus (v2.3.3)。
- **结构**:
    - Collection: `kb_{id}` (每个知识库一个集合)。
    - Fields: `chunk_id` (Int64), `vector` (FloatVector, 1536 dim), `doc_id` (Int64).
    - Index: `IVF_FLAT` (Cosine Metric).
- **功能**: 创建集合、插入向量、相似度检索。

### 2.4 RAG 服务 (Retrieval & Prompt)
- **位置**: `api-backend/src/main/java/com/sspku/agent/module/knowledge/service/impl/RagServiceImpl.java`
- **配置**: `RagConfig` (TopK, Threshold, MaxContextLength)。
- **流程**:
    1.  **Retrieve**: 根据 Query 向量在 Milvus 中检索 Top-K 相似文档块。
    2.  **Filter**: 过滤低于相似度阈值的结果。
    3.  **Fetch**: 从 MySQL 获取对应的文本内容。
    4.  **Build Prompt**: 拼接上下文和用户问题，生成最终提示词。

## 3. 数据库变更
- **新增表**: `knowledge_chunk` (存储切分后的文本)。
- **启用表**: `knowledge_base`, `document` (取消注释)。
- **修改表**: `agent` 表新增 `rag_config` 字段 (JSON)。

## 4. 配置变更
- **环境变量**: 新增 `TONGYI_API_KEY`。
- **配置文件**: `application-dev.yml` 集成 Milvus 和 Embedding 配置。

## 5. 后续建议
1.  **对话集成**: 在 `ChatController` 中调用 `RagService`，实现真正的 RAG 对话。
2.  **混合检索**: 引入关键词检索 (BM25) 与向量检索结合，提高召回率。
3.  **重排序 (Rerank)**: 引入 Rerank 模型对检索结果进行精细排序。
4.  **知识库关联**: 完善 `Agent` 与 `KnowledgeBase` 的多对多关联管理。
