# 🔌 插件系统 API 接口文档

**Base URL**: `/api/plugin`

## 1. 插件管理

### 1.1 获取插件列表
*   **URL**: `GET /api/plugin`
*   **参数**:
    *   `type` (可选): 插件类型 (`builtin` / `custom`)
    *   `status` (可选): 状态 (`enabled` / `disabled`)
*   **响应**:
    ```json
    {
      "code": 0,
      "data": [
        {
          "id": 1,
          "name": "IoT设备控制",
          "type": "custom",
          "status": "enabled",
          "description": "...",
          "openapiSpec": "...",
          "createdAt": "2023-..."
        }
      ]
    }
    ```

### 1.2 获取插件详情
*   **URL**: `GET /api/plugin/{id}`
*   **路径参数**:
    *   `id` (必填): 插件ID
*   **响应**:
    ```json
    {
      "code": 0,
      "data": {
        "id": 1,
        "name": "...",
        "type": "custom",
        "status": "enabled",
        "config": "...",
        "openapiSpec": "..."
      }
    }
    ```

### 1.3 创建插件
*   **URL**: `POST /api/plugin`
*   **Body**:
    ```json
    {
      "name": "插件名称",
      "description": "描述",
      "openapiSpec": "JSON字符串（OpenAPI 3.0 规范）",
      "config": "JSON字符串（配置信息）"
    }
    ```
*   **响应**: 成功创建的插件对象

### 1.4 更新插件
*   **URL**: `PUT /api/plugin/{id}`
*   **路径参数**: `id`
*   **Body**: 同创建接口
*   **响应**: 更新后的插件对象

### 1.5 删除插件
*   **URL**: `DELETE /api/plugin/{id}`
*   **路径参数**: `id`
*   **说明**: 只能删除自定义插件 (`type=custom`)。删除为软删除。

### 1.6 启用/禁用
*   **启用 URL**: `POST /api/plugin/{id}/enable`
*   **禁用 URL**: `POST /api/plugin/{id}/disable`
*   **响应**: `code: 0` 表示成功

## 2. 插件执行（调试与Agent调用）

### 2.1 执行插件函数
*   **URL**: `POST /api/plugin/{id}/execute`
*   **路径参数**: `id` (插件ID)
*   **Body**:
    ```json
    {
      "functionName": "operationId (如 getSensorData)",
      "arguments": {
        "param1": "value1",
        "param2": "value2"
      }
    }
    ```
*   **响应**:
    ```json
    {
      "code": 0,
      "data": {
        "pluginId": 1,
        "functionName": "getSensorData",
        "result": "{\"temp\": 25.6}"  // 执行结果字符串（JSON格式）
      }
    }
    ```

## 3. 内置插件说明

### 3.1 数据转换插件
*   **ID**: (系统生成)
*   **Type**: `builtin`
*   **函数**:
    *   `extract`: 使用 JSONPath 提取数据
        *   参数: `source` (JSON源), `path` (JSONPath表达式)
    *   `format`: 字符串模版格式化
        *   参数: `template` (模版), `data` (数据对象)

### 3.2 条件判断插件
*   **ID**: (系统生成)
*   **Type**: `builtin`
*   **函数**:
    *   `compare`: 比较两个值
        *   参数: `val1`, `val2`, `op` (eq, ne, gt, lt, ge, le, contains)

