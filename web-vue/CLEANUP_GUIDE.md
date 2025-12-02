# 前端代码清理指南

## 📋 清理步骤

### 1. 备份（可选）

建议先提交当前代码到 Git，以防需要回滚：

```bash
git add .
git commit -m "重构：完成前端架构优化"
```

### 2. 删除旧文件

#### 删除旧的控制台页面

以下文件已迁移到新位置，可以安全删除：

```bash
# 删除旧的控制台页面
Remove-Item src\views\console\AgentConsole.vue
Remove-Item src\views\console\PluginConsole.vue
Remove-Item src\views\console\AgentDetail.vue
Remove-Item src\views\console\PluginDetail.vue
```

迁移对照表：

- `views/console/AgentConsole.vue` → `views/agents/index.vue` ✅
- `views/console/PluginConsole.vue` → `views/plugins/index.vue` ✅
- `views/console/AgentDetail.vue` → `views/agents/[id].vue` ✅
- `views/console/PluginDetail.vue` → `views/plugins/[id].vue` ✅

#### 删除旧的布局目录

```bash
# 删除旧的布局目录
Remove-Item -Recurse src\views\layout\
```

迁移对照表：

- `views/layout/ConsoleLayout.vue` → `layouts/MainLayout.vue` ✅

#### 删除旧的样式目录

```bash
# 删除旧的样式目录（如果已完全迁移）
Remove-Item -Recurse src\assets\styles\
```

迁移对照表：

- `assets/styles/` → `styles/` ✅（全新设计系统）

### 3. 保留的旧文件（待重构）

以下文件暂时保留，需要后续重构：

- `views/console/WorkflowDesigner.vue` - 工作流设计器
- `views/console/KnowledgeConsole.vue` - 知识库管理

### 4. 一键清理脚本

在 web-vue 目录下执行：

```powershell
# 清理旧文件（PowerShell）
$filesToDelete = @(
    "src\views\console\AgentConsole.vue",
    "src\views\console\PluginConsole.vue",
    "src\views\console\AgentDetail.vue",
    "src\views\console\PluginDetail.vue"
)

foreach ($file in $filesToDelete) {
    if (Test-Path $file) {
        Remove-Item $file -Force
        Write-Host "✓ 已删除: $file" -ForegroundColor Green
    } else {
        Write-Host "⊘ 文件不存在: $file" -ForegroundColor Yellow
    }
}

# 删除旧的布局目录
if (Test-Path "src\views\layout") {
    Remove-Item "src\views\layout" -Recurse -Force
    Write-Host "✓ 已删除: src\views\layout\" -ForegroundColor Green
}

Write-Host "`n清理完成！" -ForegroundColor Cyan
```

### 5. 验证清理结果

```powershell
# 检查是否还有旧文件引用
Get-ChildItem -Path src -Recurse -Include *.vue,*.ts | Select-String "views/console/Agent|views/console/Plugin|views/layout/Console"
```

如果有结果输出，说明还有文件在引用旧路径，需要手动更新。

## 🔍 清理检查清单

- [ ] 删除 `views/console/AgentConsole.vue`
- [ ] 删除 `views/console/PluginConsole.vue`
- [ ] 删除 `views/console/AgentDetail.vue`
- [ ] 删除 `views/console/PluginDetail.vue`
- [ ] 删除 `views/layout/` 目录
- [ ] 删除 `assets/styles/` 目录（可选）
- [ ] 验证没有旧路径引用
- [ ] 测试所有页面路由正常工作
- [ ] 测试所有功能正常

## ⚠️ 注意事项

1. **Git 版本控制**：删除前确保代码已提交到 Git
2. **增量清理**：建议分批删除，每删除一批就测试一次
3. **路径检查**：删除后搜索代码，确保没有遗留的旧路径引用
4. **功能测试**：删除后全面测试功能是否正常

## 🚀 清理后的目录结构

```
src/
├── components/        # ✨ 组件库
│   ├── common/       # 通用组件
│   ├── agent/        # 智能体组件
│   └── plugin/       # 插件组件
├── layouts/           # ✨ 布局组件
│   └── MainLayout.vue
├── styles/            # ✨ 样式系统
│   ├── variables.css
│   ├── utilities.css
│   └── ...
└── views/             # ✨ 页面视图
    ├── agents/       # 智能体模块
    │   ├── index.vue
    │   └── [id].vue
    ├── plugins/      # 插件模块
    │   ├── index.vue
    │   └── [id].vue
    ├── auth/         # 认证模块
    └── console/      # 其他控制台页面（待重构）
        ├── WorkflowDesigner.vue
        └── KnowledgeConsole.vue
```

## 📊 清理收益

- **减少代码量**：删除约 800+ 行冗余代码
- **提升维护性**：单一职责，结构清晰
- **加快构建**：减少不必要的文件打包
- **降低困惑**：新旧文件分离，避免混淆

---

清理完成后，建议再次运行 `npm run build` 确保项目可以正常构建。
