#!/bin/bash
# ============================================================
#  03-deploy.sh
#  功能: 部署 / 更新应用（Ubuntu 20 版本）
#  执行: sudo bash 03-deploy.sh [jar路径] [dist目录路径]
#
#  示例:
#    sudo bash 03-deploy.sh \
#      /tmp/ai-token-backend.jar \
#      /tmp/dist
# ============================================================
set -e

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC}  $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }
step() { echo -e "\n${BLUE}──────── $* ────────${NC}"; }

JAR_SRC="${1:-/tmp/ai-token-backend.jar}"
DIST_SRC="${2:-/tmp/dist}"

DEPLOY_DIR="/opt/ai-token"
BACKEND_DIR="${DEPLOY_DIR}/backend"
DIST_DIR="${DEPLOY_DIR}/dist"
LOG_DIR="${DEPLOY_DIR}/logs"
SERVICE_NAME="ai-token-backend"
TEMPLATE_UNIT="ai-token-backend@.service"
INSTANCES="8082 8083"
SCRIPT_DIR="$(cd "$(dirname "$0")"; pwd)"

# ── Step 1: 检查文件 ─────────────────────────────────────
step "检查部署文件"
[ -f "$JAR_SRC"  ] || err "JAR 文件不存在: ${JAR_SRC}"
[ -d "$DIST_SRC" ] || err "dist 目录不存在: ${DIST_SRC}"
log "JAR:  ${JAR_SRC}"
log "dist: ${DIST_SRC}"

# ── Step 1.5: 确保系统用户和目录存在 ────────────────────
step "初始化用户与目录"
if ! id ai-token &>/dev/null; then
    log "创建系统用户 ai-token ..."
    adduser --system --no-create-home --group ai-token
else
    log "用户 ai-token 已存在，跳过"
fi
mkdir -p "${BACKEND_DIR}" "${DIST_DIR}" "${LOG_DIR}"
chown -R ai-token:ai-token "${DEPLOY_DIR}"
chmod 755 "${DEPLOY_DIR}"
log "目录就绪: ${DEPLOY_DIR}"

# ── Step 2: 首次部署 - 创建 .env ─────────────────────────
step "检查配置文件"
if [ ! -f "${BACKEND_DIR}/.env" ]; then
    warn ".env 不存在，从模板创建..."
    cp "${SCRIPT_DIR}/../config/.env.example" "${BACKEND_DIR}/.env"
    chmod 600 "${BACKEND_DIR}/.env"
    chown ai-token:ai-token "${BACKEND_DIR}/.env"
    warn "请编辑 ${BACKEND_DIR}/.env 填写正确的数据库密码！"
    warn "  nano ${BACKEND_DIR}/.env"
    read -p "配置完成后按 [Enter] 继续..."
else
    log ".env 已存在，跳过"
fi

# ── Step 3: 同步生产配置 ─────────────────────────────────
step "同步生产配置"
cp "${SCRIPT_DIR}/../config/application-prod.yml" "${BACKEND_DIR}/application-prod.yml"
chown ai-token:ai-token "${BACKEND_DIR}/application-prod.yml"
log "application-prod.yml 已同步"

# ── Step 4: 停止旧服务 ───────────────────────────────────
step "停止旧服务"
if systemctl is-active --quiet "$SERVICE_NAME" 2>/dev/null; then
    log "停止旧单实例服务 ${SERVICE_NAME} ..."
    systemctl stop    "$SERVICE_NAME"
    systemctl disable "$SERVICE_NAME" 2>/dev/null || true
fi
for PORT in $INSTANCES; do
    INST="${SERVICE_NAME}@${PORT}"
    if systemctl is-active --quiet "$INST" 2>/dev/null; then
        log "停止 ${INST} ..."
        systemctl stop "$INST"
    fi
done
sleep 1

# ── Step 5: 部署后端 JAR ─────────────────────────────────
step "部署后端 JAR"
if [ -f "${BACKEND_DIR}/ai-token-backend.jar" ]; then
    BACKUP="${BACKEND_DIR}/ai-token-backend.jar.$(date +%Y%m%d%H%M%S).bak"
    mv "${BACKEND_DIR}/ai-token-backend.jar" "$BACKUP"
    log "旧 JAR 已备份: ${BACKUP}"
    ls -t "${BACKEND_DIR}"/*.bak 2>/dev/null | tail -n +4 | xargs -r rm -f
fi
cp "$JAR_SRC" "${BACKEND_DIR}/ai-token-backend.jar"
chown ai-token:ai-token "${BACKEND_DIR}/ai-token-backend.jar"
log "后端 JAR 部署完成"

# ── Step 6: 部署前端静态文件 ─────────────────────────────
step "部署前端文件"
rm -rf "${DIST_DIR:?}"/*
cp -r "${DIST_SRC}/." "${DIST_DIR}/"
chown -R www-data:www-data "${DIST_DIR}"
find "${DIST_DIR}" -type d -exec chmod 755 {} \;
find "${DIST_DIR}" -type f -exec chmod 644 {} \;
log "前端文件部署到 ${DIST_DIR} 完成"

# ── Step 7: 配置 Nginx ────────────────────────────────────
step "配置 Nginx"
if ! command -v nginx &>/dev/null; then
    log "Nginx 未安装，正在安装..."
    apt-get update -qq
    apt-get install -y nginx
    systemctl enable nginx
    systemctl start  nginx
fi

mkdir -p /etc/nginx/conf.d
cp "${SCRIPT_DIR}/../nginx/ai-token.conf" /etc/nginx/conf.d/ai-token.conf
rm -f /etc/nginx/sites-enabled/default

nginx -t && log "Nginx 配置验证通过" || err "Nginx 配置有误，请检查"
systemctl reload nginx
log "Nginx 已重载"

# ── Step 8: 启动后端服务（双实例）───────────────────────
step "启动后端服务（双实例）"
cp "${SCRIPT_DIR}/../systemd/${TEMPLATE_UNIT}" \
   /etc/systemd/system/
systemctl daemon-reload

for PORT in $INSTANCES; do
    INST="${SERVICE_NAME}@${PORT}"
    systemctl enable "$INST"
    systemctl start  "$INST"
    log "已启动 ${INST}"
done

log "等待两个实例就绪..."
for PORT in $INSTANCES; do
    ok=0
    for i in $(seq 1 12); do
        sleep 5
        if curl -sf "http://127.0.0.1:${PORT}/api/auth/login" \
                -X POST -H 'Content-Type: application/json' \
                -d '{}' &>/dev/null; then
            log "实例 :${PORT} 已就绪 ✓"
            ok=1
            break
        fi
    done
    [ "$ok" -eq 0 ] && warn "实例 :${PORT} 启动超时，请查看日志: journalctl -u ${SERVICE_NAME}@${PORT} -n 50"
done

# ── 摘要 ─────────────────────────────────────────────────
echo ""
log "========================================"
log "  部署完成！"
log ""
log "  前端: http://<服务器IP>/ai-token/"
log "  API:  http://<服务器IP>/ai-token/api"
log "  代理: http://<服务器IP>/ai-token/v1/chat/completions"
log ""
log "  实例1 状态: systemctl status ${SERVICE_NAME}@8082"
log "  实例2 状态: systemctl status ${SERVICE_NAME}@8083"
log "  实例1 日志: tail -f ${LOG_DIR}/ai-token-8082.log"
log "  实例2 日志: tail -f ${LOG_DIR}/ai-token-8083.log"
log "========================================"
