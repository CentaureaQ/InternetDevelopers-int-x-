#!/bin/bash

# ==================== 容器化自动化部署脚本 ====================
# 特点：完全在容器内构建，无需安装任何开发环境
# 适用于：生产服务器、CI/CD 流水线

set -e  # 遇到错误立即退出

# ==================== 颜色定义 ====================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'  # 无颜色

# ==================== 日志函数 ====================
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_step() {
    echo -e "\n${BLUE}==================== $1 ====================${NC}\n"
}

# ==================== 检查命令是否存在 ====================
check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "$1 未安装，请先安装 $1"
        exit 1
    fi
}

# ==================== 0. 数据库初始化 (可选) ====================
if [ "$1" == "init-db" ]; then
    log_step "0. 数据库初始化"
    if [ -f "./init-db.sh" ]; then
        chmod +x ./init-db.sh
        ./init-db.sh
    else
        log_error "找不到 init-db.sh 脚本"
    fi
fi

# ==================== 开始部署 ====================
log_step "开始部署"
log_info "时间: $(date '+%Y-%m-%d %H:%M:%S')"

# ==================== 1. 环境检查 ====================
log_step "1. 环境检查"

log_info "检查 Docker..."
check_command docker
docker --version

log_info "检查 Docker Compose..."
check_command docker-compose
docker-compose --version

log_info "检查 .env 文件..."
if [ ! -f .env ]; then
    log_error ".env 文件不存在，请先创建配置文件"
    exit 1
fi
log_info ".env 文件存在 ✓"

# ==================== 2. 准备 Maven 缓存 ====================
log_step "2. 准备 Maven 缓存"

MAVEN_CACHE_VOLUME="health-agent-maven-repo"

if docker volume inspect $MAVEN_CACHE_VOLUME &> /dev/null; then
    log_info "Maven 缓存卷已存在: $MAVEN_CACHE_VOLUME"
else
    log_info "创建 Maven 缓存卷: $MAVEN_CACHE_VOLUME"
    docker volume create --name $MAVEN_CACHE_VOLUME
fi

# ==================== 3. 容器化构建后端 JAR 包 ====================
log_step "3. 容器化构建后端 JAR 包"

log_info "使用 Maven Docker 容器构建后端..."
log_warn "首次构建可能需要 5-10 分钟（下载依赖）"

docker run --rm \
    -v $MAVEN_CACHE_VOLUME:/root/.m2 \
    -v "$PWD/../api-backend":/usr/src/app \
    -w /usr/src/app \
    maven:3.8-eclipse-temurin-17 \
    mvn clean package -DskipTests

# 检查 JAR 包是否生成
if ls ../api-backend/target/*.jar 1> /dev/null 2>&1; then
    log_info "后端 JAR 包构建成功 ✓"
    ls -lh ../api-backend/target/*.jar
else
    log_error "后端 JAR 包构建失败"
    exit 1
fi

# ==================== 4. 构建 Docker 镜像 ====================
log_step "4. 构建 Docker 镜像"

log_info "开始构建所有服务的 Docker 镜像..."
log_warn "前端构建可能需要 5-10 分钟（下载 npm 依赖）"

# 预拉取基础镜像，减少构建时间]【以及如果不提前这么写的话，有可能构建报错】
docker pull eclipse-temurin:17-jre
docker pull node:18-alpine
docker pull nginx:alpine

# 构建时不使用缓存，确保获取最新代码
docker-compose build --no-cache

log_info "所有镜像构建完成 ✓"

# ==================== 5. 停止旧服务 ====================
log_step "5. 停止旧服务"

if docker-compose ps | grep -q "Up"; then
    log_warn "检测到正在运行的服务，停止中..."
    docker-compose down
    log_info "旧服务已停止 ✓"
else
    log_info "没有正在运行的服务"
fi

# ==================== 6. 启动新服务 ====================
log_step "6. 启动新服务"

log_info "启动所有服务..."
docker-compose up -d

# 等待服务启动
log_info "等待服务启动..."
sleep 5

# ==================== 7. 验证部署 ====================
log_step "7. 验证部署"

log_info "检查服务状态..."
docker-compose ps

# ==================== 8. 部署完成 ====================
log_step "部署完成！"

echo -e "${GREEN}"
echo "========================================"
echo "✓ 部署成功！服务访问地址："
echo "========================================"
echo "前端管理端:  http://localhost"
echo "后端 API:    http://localhost:8080"
echo "API 文档:    http://localhost:8080/doc.html"
echo "PHPMyAdmin:  http://localhost:8081"
echo "========================================"
echo -e "${NC}"

log_info "查看日志命令: docker-compose logs -f"
log_info "停止服务命令: docker-compose down"