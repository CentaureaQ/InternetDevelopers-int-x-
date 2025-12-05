#!/bin/bash

# ==================== 数据库初始化脚本 ====================
# 功能：使用 Docker 容器运行 MySQL 客户端，连接远程数据库并执行初始化脚本
# 前提：需要安装 Docker

set -e

# ==================== 配置 ====================
# 加载 .env 文件
if [ -f "$SCRIPT_DIR/.env" ]; then
    set -a
    source "$SCRIPT_DIR/.env"
    set +a
fi

DB_HOST="${DB_HOST:-10.129.83.19}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USERNAME:-demo_user}"
DB_PASS="${DB_PASSWORD:-demo_pass_123}"
DB_NAME="${MYSQL_DATABASE:-ai_platform}"

# 脚本所在目录的绝对路径
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
SQL_DIR="$PROJECT_ROOT/database/init"

# ==================== 颜色定义 ====================
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# ==================== 检查 Docker ====================
if ! command -v docker &> /dev/null; then
    log_error "Docker 未安装，无法运行此脚本"
    exit 1
fi

# ==================== 执行 SQL ====================
log_info "开始初始化数据库..."
log_info "数据库地址: $DB_HOST:$DB_PORT"
log_info "SQL 目录: $SQL_DIR"

# 使用 mysql 镜像运行一次性容器
# --network host 确保可以访问外部网络（虽然默认 bridge 也可以，但 host 更稳妥）
# -v 挂载 SQL 目录
docker run --rm -i \
    -v "$SQL_DIR":/sql \
    mysql:8.0 \
    bash -c "
        echo '正在连接数据库...'
        # 等待数据库连接（可选，这里直接执行）
        
        echo '1. 执行 Schema 初始化 (01-schema.sql)...'
        mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS < /sql/01-schema.sql
        
        echo '2. 执行初始数据 (02-data.sql)...'
        mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME < /sql/02-data.sql
        
        echo '3. 执行知识库表结构 (03-knowledge.sql)...'
        mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME < /sql/03-knowledge.sql
        
        echo '数据库初始化完成！'
    "

if [ $? -eq 0 ]; then
    log_info "数据库同步成功！"
else
    log_error "数据库同步失败，请检查连接信息或 SQL 脚本。"
    exit 1
fi
