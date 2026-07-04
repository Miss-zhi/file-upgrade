#!/bin/bash
# ============================================================
# 奇文网盘 — 一键部署脚本
# 用法:
#   ./deploy.sh          # 首次部署（构建 + 启动）
#   ./deploy.sh rebuild  # 重新构建镜像并启动
#   ./deploy.sh restart  # 仅重启（不重新构建）
#   ./deploy.sh logs     # 查看所有服务日志
#   ./deploy.sh stop     # 停止所有服务
#   ./deploy.sh update   # 拉取最新代码并重新部署
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.yml"
ENV_FILE="$SCRIPT_DIR/.env"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; }

# 检查 .env 文件
check_env() {
    if [ ! -f "$ENV_FILE" ]; then
        error "未找到 .env 文件"
        info "请复制模板并填写实际值:"
        info "  cp $SCRIPT_DIR/.env.example $ENV_FILE"
        info "  vim $ENV_FILE"
        exit 1
    fi
}

# 检查 Docker
check_docker() {
    if ! command -v docker &>/dev/null; then
        error "未安装 Docker，请先安装 Docker 和 Docker Compose"
        exit 1
    fi
    if ! docker compose version &>/dev/null; then
        error "未安装 Docker Compose V2 插件"
        exit 1
    fi
}

# 部署
deploy() {
    check_env
    check_docker

    info "开始部署奇文网盘..."
    info "工作目录: $SCRIPT_DIR"

    cd "$SCRIPT_DIR"

    if [ "${1:-}" = "rebuild" ]; then
        info "重新构建所有镜像..."
        docker compose -f "$COMPOSE_FILE" build --no-cache
    else
        info "构建/更新镜像..."
        docker compose -f "$COMPOSE_FILE" build
    fi

    info "启动服务..."
    docker compose -f "$COMPOSE_FILE" up -d

    info "部署完成！等待服务健康检查..."
    sleep 5

    docker compose -f "$COMPOSE_FILE" ps

    info ""
    info "========================================="
    info " 部署成功！"
    info " 前端地址: http://$(hostname -I | awk '{print $1}')"
    info " 后端地址: http://$(hostname -I | awk '{print $1}'):8080"
    info "========================================="
    info ""
    info "常用命令:"
    info "  查看日志:  docker compose -f $COMPOSE_FILE logs -f"
    info "  重启服务:  docker compose -f $COMPOSE_FILE restart"
    info "  停止服务:  docker compose -f $COMPOSE_FILE down"
}

# 查看日志
show_logs() {
    check_env
    cd "$SCRIPT_DIR"
    docker compose -f "$COMPOSE_FILE" logs -f --tail=200 "${1:-}"
}

# 停止
stop() {
    check_env
    cd "$SCRIPT_DIR"
    info "停止所有服务..."
    docker compose -f "$COMPOSE_FILE" down
    info "已停止"
}

# 更新代码并部署
update() {
    check_env
    cd "$PROJECT_DIR"

    info "拉取最新代码..."
    if [ -d ".git" ]; then
        git pull
    else
        warn "非 Git 仓库，跳过代码更新"
    fi

    deploy rebuild
}

# 主逻辑
case "${1:-deploy}" in
    deploy|build)
        deploy
        ;;
    rebuild)
        deploy rebuild
        ;;
    restart)
        check_env
        cd "$SCRIPT_DIR"
        docker compose -f "$COMPOSE_FILE" restart
        info "已重启"
        ;;
    logs)
        show_logs "${2:-}"
        ;;
    stop|down)
        stop
        ;;
    update)
        update
        ;;
    status)
        check_env
        cd "$SCRIPT_DIR"
        docker compose -f "$COMPOSE_FILE" ps
        ;;
    *)
        echo "用法: $0 {deploy|rebuild|restart|logs|stop|update|status}"
        echo ""
        echo "  deploy   首次部署（构建 + 启动）[默认]"
        echo "  rebuild  重新构建镜像并启动"
        echo "  restart  仅重启（不重新构建）"
        echo "  logs     查看服务日志"
        echo "  stop     停止所有服务"
        echo "  update   拉取最新代码并重新部署"
        echo "  status   查看服务状态"
        exit 1
        ;;
esac
