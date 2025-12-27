# 智能体创作平台 - Web前端

基于 Vue 3 + TypeScript + Element Plus 的Web前端应用。

## 技术栈

- **Vue 3** - 渐进式JavaScript框架
- **TypeScript** - 类型安全的JavaScript超集
- **Element Plus** - Vue 3组件库
- **Vite** - 下一代前端构建工具
- **Pinia** - Vue状态管理
- **Vue Router** - 官方路由管理器
- **Axios** - HTTP客户端

## 项目结构

```
web-vue/
├── src/
│   ├── main.ts                  # 入口文件
│   ├── App.vue                  # 根组件
│   ├── views/                   # 页面组件
│   │   ├── auth/               # 认证页面
│   │   ├── console/            # 控制台页面
│   │   └── layout/             # 布局组件
│   ├── components/             # 公共组件
│   ├── stores/                  # Pinia状态管理
│   ├── api/                     # API调用
│   ├── utils/                   # 工具函数
│   └── assets/                 # 静态资源
├── package.json
└── vite.config.ts
```

## 开发

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:5174

### 构建生产版本

```bash
npm run build
```

## 功能模块

- **智能体编辑器** - 创建、编辑、测试智能体
- **工作流设计器** - 可视化工作流编排
- **知识库管理** - 文档上传、向量化存储
- **插件配置** - 插件管理和配置

## 后端接口

前端通过 `/api` 代理到后端服务（默认 http://localhost:8080），确保前后端联调。



