# 工作流（MVP+，图执行，纯文本为主）设计说明

目标：在当前项目中落地一个可扩展的工作流：支持前端画布编辑 + 后端 CRUD + 调试运行（Debug）。节点类型与字段参考《节点类型与数据结构说明文档》，但本项目先支持其中一批“基础可跑通”的节点，并以 graph JSON（与 web-vue 编辑器一致）做存储与执行。

---

## 1. 当前支持的节点类型（对齐文档命名）

已支持（最小字段集）：

- `startNodeStart`（兼容旧 `start`）：开始
- `knowledgeRetrievalNodeState`：知识检索（MVP 先复用现有 RagService，输出到 `vars`）
- `textConcatenationNodeState`：文本串联（按行模板渲染并拼接）
- `variableAggregationNodeState`：变量聚合（MVP 先等同于“串联”，输出 key 默认 `result`）
- `variableUpdaterNodeState`：变量更新（写入 `vars.<targetKey>`）
- `llmNodeState`（兼容旧 `llm`）：大模型
- `endNodeEnd`（兼容旧 `end`）：结束

约束（基础校验）：

- 必须且只能有 1 个开始节点（`start` 或 `startNodeStart`）
- 必须且只能有 1 个结束节点（`end` 或 `endNodeEnd`）
- 其他节点数量不做限制（执行时需满足 DAG，无环）

---

## 2. Graph 数据结构（与前端 editor 产物一致）

存储为字符串（JSON）到数据库字段 `workflow.graph`。

```json
{
  "version": "1.0",
  "nodes": [
    { "id": "start", "type": "startNodeStart", "x": 80, "y": 120 },
    {
      "id": "knowledge",
      "type": "knowledgeRetrievalNodeState",
      "x": 420,
      "y": 120,
      "queryTemplate": "{{inputs.query}}",
      "agentIdKey": "agentId",
      "knowledgeOutputKey": "knowledge"
    },
    {
      "id": "llm",
      "type": "llmNodeState",
      "x": 760,
      "y": 120,
      "model": "qwen-max-latest",
      "prompt": "问题：{{inputs.query}}\n知识：{{vars.knowledge}}",
      "llmOutputKey": "llmOutput"
    },
    {
      "id": "end",
      "type": "endNodeEnd",
      "x": 1100,
      "y": 120,
      "outputKey": "answer"
    }
  ],
  "edges": [
    { "from": "start", "to": "knowledge" },
    { "from": "knowledge", "to": "llm" },
    { "from": "llm", "to": "end" }
  ]
}
```

字段说明：

- `version`: graph schema 版本（当前固定 `1.0`）
- `nodes`: 节点列表
  - `id`: 节点 id（前端默认 `start/llm/end`）
  - `type`: `start | llm | end`
  - `x/y`: 画布坐标
  - `model/prompt`: `llm` 节点配置
  - `outputKey`: `end` 节点配置
- `edges`: 边（用于执行顺序：后端按拓扑排序执行；若 edges 为空则按 x/y 排序兜底）

---

## 3. 执行模型（Debug）

### 3.1 输入

前端调试面板提交：

```json
{ "inputs": { "query": "你好" } }
```

后端约定：

- 输入总是挂在 `inputs` 对象下
- 执行中产生的中间结果写入 `vars`（例如 `vars.knowledge`、`vars.llmOutput`）

### 3.2 Prompt 模板渲染（MVP）

模板变量支持：

- `{{inputs.xxx}}` / `{{inputs.a.b}}`
- `{{vars.xxx}}` / `{{vars.a.b}}`

缺失字段渲染为空字符串；非字符串值会用 JSON 串渲染。

### 3.3 输出

- 若存在 `llm` 节点：取 LLM 的纯文本输出 `reply`
- `end` 节点将输出包装为：

```json
{ "<outputKey>": "<reply>" }
```

默认 `outputKey = "answer"`。

### 3.4 Trace

后端返回节点级 trace，前端直接 JSON 展示：

```json
{
  "output": { "answer": "..." },
  "trace": [
    {
      "nodeId": "start",
      "nodeType": "start",
      "status": "success",
      "startedAt": 0,
      "finishedAt": 0,
      "input": { "inputs": {} },
      "output": { "inputs": {} }
    },
    {
      "nodeId": "llm",
      "nodeType": "llm",
      "status": "success",
      "input": { "model": "...", "prompt": "..." },
      "output": "..."
    },
    {
      "nodeId": "end",
      "nodeType": "end",
      "status": "success",
      "output": { "answer": "..." }
    }
  ]
}
```

---

## 4. 后端接口（与 web-vue/src/api/workflow.ts 对齐）

- `GET /api/workflows`：分页列表（支持 `pageNo/pageSize/keyword/status`）
- `GET /api/workflows/{id}`：详情
- `POST /api/workflows`：创建（返回 id）
- `PUT /api/workflows/{id}`：更新
- `DELETE /api/workflows/{id}`：删除
- `POST /api/workflows/{id}/publish`：发布
- `POST /api/workflows/{id}/unpublish`：取消发布
- `POST /api/workflows/{id}/debug`：调试运行

统一返回 `ApiResponse`（code=0 表示成功）。

---

## 5. 数据库表

表：`workflow`

关键字段：

- `owner_user_id`: 归属用户（MVP 用于权限隔离）
- `status`: `draft|published`
- `version`: 字符串（默认 `1.0`）
- `graph`: LONGTEXT（存 graph JSON 字符串）

对应建表脚本：

- `database/init/04-workflow.sql`（已纳入 deploy/init-db.sh 初始化流程）
- `database/migrations/002_create_workflow.sql`（保留作为迁移记录）
