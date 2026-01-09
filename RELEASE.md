# InternetDevelopers v1.4.0 Release

## Release说明

本版本包含完整的工作流引擎实现、RAG知识库检索、智能体管理等核心功能，支持前端可视化编辑和后端完整执行。

---

## 完整功能列表

### 第一周：基础架构搭建
- ✅ 项目结构初始化（前后端分离）
- ✅ 数据库初始化和基础表设计
- ✅ 前端Vue3项目配置
- ✅ 后端Spring Boot框架搭建

### 第二周：智能体管理模块
- ✅ 智能体CRUD接口
- ✅ 智能体配置管理
- ✅ 前端智能体列表和编辑页面
- ✅ 智能体与工作流关联

### 第三周：插件系统和API集成
- ✅ OpenAPI解析和执行引擎
- ✅ 插件动态加载和管理
- ✅ HTTP节点支持
- ✅ 外部API集成框架

### 第四周：工作流引擎核心
- ✅ 工作流图定义和存储（JSON格式）
- ✅ DAG执行引擎（拓扑排序）
- ✅ 节点动态执行框架
- ✅ 变量管理系统
- ✅ 条件分支逻辑
- ✅ 并行节点执行
- ✅ 错误处理和日志记录
- ✅ 前端工作流编辑器集成

### 第五周：RAG知识库和工作流优化
- ✅ 知识库CRUD管理
- ✅ 文档上传和处理
- ✅ 向量化存储（Milvus）
- ✅ 相似度检索
- ✅ RAG提示词构建
- ✅ 知识检索节点实现
- ✅ 工作流调试和优化
- ✅ 条件分支执行逻辑修复

### 核心支持节点类型
- `startNodeStart` - 工作流开始
- `endNodeEnd` - 工作流结束
- `knowledgeRetrievalNodeState` - 知识检索
- `textConcatenationNodeState` - 文本串联
- `variableAggregationNodeState` - 变量聚合
- `variableUpdaterNodeState` - 变量更新
- `llmNodeState` - LLM调用
- `httpNodeState` - HTTP请求
- `conditionNodeState` - 条件分支
- `parallelLoopNodeState` - 并行循环

---

## 系统架构说明

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    前端层 (Vue3 + Vite)                      │
│  - 工作流可视化编辑器                                        │
│  - 智能体管理界面                                            │
│  - 知识库管理界面                                            │
│  - 工作流调试工具                                            │
└────────────────────┬────────────────────────────────────────┘
                     │ HTTP/REST API
┌────────────────────▼────────────────────────────────────────┐
│                   后端层 (Spring Boot)                       │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  控制层 (Controller)                                 │   │
│  │  - AgentController                                  │   │
│  │  - WorkflowController                              │   │
│  │  - KnowledgeController                             │   │
│  │  - PluginController                                │   │
│  └────────────────────┬─────────────────────────────────┘   │
│  ┌────────────────────▼─────────────────────────────────┐   │
│  │  业务逻辑层 (Service)                                │   │
│  │  - AgentService                                     │   │
│  │  - WorkflowExecutionEngine                         │   │
│  │  - KnowledgeService / RagService                   │   │
│  │  - PluginService                                   │   │
│  │  - EmbeddingService                                │   │
│  └────────────────────┬─────────────────────────────────┘   │
│  ┌────────────────────▼─────────────────────────────────┐   │
│  │  数据访问层 (Mapper/Repository)                      │   │
│  │  - AgentMapper                                      │   │
│  │  - WorkflowMapper                                  │   │
│  │  - KnowledgeMapper                                 │   │
│  └────────────────────┬─────────────────────────────────┘   │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
┌───────▼──┐  ┌──────▼──┐  ┌─────▼────┐
│  MySQL   │  │ Milvus  │  │  Redis   │
│  业务DB  │  │ 向量DB  │  │  缓存    │
└──────────┘  └─────────┘  └──────────┘
```

### 核心模块说明

#### 1. 工作流执行引擎 (Workflow Execution Engine)
- **位置**: `api-backend/src/main/java/com/sspku/agent/module/workflow/`
- **核心类**: `WorkflowExecutor`, `WorkflowExecutionContext`, `NodeFactory`
- **功能**:
  - 图解析与验证
  - 拓扑排序执行顺序
  - 节点动态实例化
  - 变量生命周期管理
  - 错误处理与回滚

#### 2. RAG知识库系统 (RAG Knowledge System)
- **位置**: `api-backend/src/main/java/com/sspku/agent/module/knowledge/`
- **核心功能**:
  - 文档分块 (Chunking): TextChunker, MarkdownChunker
  - 向量化 (Embedding): TongyiEmbeddingService
  - 向量存储 (Vector Store): MilvusVectorStorageService
  - RAG检索: RagService (Retrieve-Augmented Generation)
- **数据结构**:
  - `knowledge_base`: 知识库表
  - `document`: 文档表
  - `knowledge_chunk`: 文本块表
  - Milvus Collection: `kb_{id}`

#### 3. 智能体管理 (Agent Management)
- **位置**: `api-backend/src/main/java/com/sspku/agent/module/agent/`
- **功能**:
  - 智能体CRUD
  - 配置管理 (RAG配置、模型配置)
  - 工作流绑定
  - 对话历史管理

#### 4. 插件系统 (Plugin System)
- **位置**: `api-backend/src/main/java/com/sspku/agent/module/plugin/`
- **功能**:
  - OpenAPI解析
  - 插件动态加载
  - 插件执行框架
  - API调用代理

#### 5. 前端编辑器 (Frontend Editor)
- **位置**: `web-vue/src/`
- **功能**:
  - 可视化工作流编辑
  - 节点拖拽和连接
  - 工作流执行和调试
  - 智能体和知识库管理

---

## 完整部署文档链接

- **完整部署指南**: [deploy/DEPLOY.md](deploy/DEPLOY.md)
- **Docker容器化部署**: 见 `deploy/docker-compose.yml`
- **数据库初始化**: `database/init/` 目录
- **环境配置示例**: `deploy/.env.example`

### 快速部署命令
```bash
cd deploy
make deploy  # 一键部署所有服务
```

---

## 用户使用手册链接

- **前端快速开始**: [FRONTEND_RAG_QUICKSTART.md](FRONTEND_RAG_QUICKSTART.md)
- **工作流节点使用说明**: [工作节点使用说明.md](工作节点使用说明.md)
- **节点类型与数据结构**: [节点类型与数据结构说明文档.md](节点类型与数据结构说明文档.md)
- **工作流设计文档**: [WORKFLOW_MVP_DESIGN.md](WORKFLOW_MVP_DESIGN.md)
- **RAG功能说明**: [RAG_SUMMARY.md](RAG_SUMMARY.md)
- **前端实现细节**: [FRONTEND_RAG_IMPLEMENTATION.md](FRONTEND_RAG_IMPLEMENTATION.md)
- **集成检查清单**: [INTEGRATION_CHECKLIST.md](INTEGRATION_CHECKLIST.md)

---

## 技术栈

- **后端**: Java 8+, Spring Boot 2.7+, MyBatis
- **前端**: Vue 3, TypeScript, Vite
- **数据库**: MySQL 8.0, Milvus 2.3+
- **缓存**: Redis
- **容器化**: Docker, Docker Compose
- **LLM服务**: 通义千问 API, OpenAI兼容接口

---

## 版本信息

- **版本号**: v1.4.0
- **发布日期**: 2026-01-09
- **主要特性**: 工作流引擎完整实现、RAG知识库系统、智能体管理、插件系统
- **支持的节点类型**: 10+种

---

## 相关文档汇总

| 文档 | 描述 |
|------|------|
| [DEPLOY.md](deploy/DEPLOY.md) | 完整部署指南 |
| [WORKFLOW_MVP_DESIGN.md](WORKFLOW_MVP_DESIGN.md) | 工作流设计说明 |
| [工作节点使用说明.md](工作节点使用说明.md) | 节点使用指南 |
| [节点类型与数据结构说明文档.md](节点类型与数据结构说明文档.md) | 详细的节点和数据结构定义 |
| [RAG_SUMMARY.md](RAG_SUMMARY.md) | RAG系统实现总结 |
| [FRONTEND_RAG_QUICKSTART.md](FRONTEND_RAG_QUICKSTART.md) | 前端快速开始 |
| [FRONTEND_RAG_IMPLEMENTATION.md](FRONTEND_RAG_IMPLEMENTATION.md) | 前端实现细节 |
| [BACKEND_CHANGES_KNOWLEDGE_STEP1.md](BACKEND_CHANGES_KNOWLEDGE_STEP1.md) | 后端知识库模块变更 |
| [INTEGRATION_CHECKLIST.md](INTEGRATION_CHECKLIST.md) | 集成检查清单 |
| [FRONTEND_RAG_CHANGELOG.md](FRONTEND_RAG_CHANGELOG.md) | 前端更新日志 |
