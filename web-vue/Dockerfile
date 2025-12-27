# ==================== 第一阶段：构建前端 ====================
# ⚠️ 注意：Vite 4+ 需要 Node.js 18+，不要使用 Node 16！
FROM node:18-alpine AS build-stage

WORKDIR /app

# 复制 npm 配置文件（如果存在）
COPY .npmrc* ./

# 复制 package.json 和 lock 文件
COPY package*.json ./

# 安装依赖（使用国内镜像源）
RUN npm install --registry=https://registry.npmmirror.com

# 复制所有源代码
COPY . .

# 构建生产版本
# ⚠️ 根据实际项目的 package.json 调整构建命令
ARG NODE_ENV=production
RUN npm run build

# ==================== 第二阶段：Nginx 运行 ====================
FROM nginx:alpine

LABEL maintainer="health-agent-platform"

# 设置时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 从构建阶段复制打包文件
# ⚠️ 根据实际项目的构建输出目录调整（dist、build 等）
COPY --from=build-stage /app/dist /usr/share/nginx/html

# 暴露端口
EXPOSE 80

# 健康检查（可选）
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]