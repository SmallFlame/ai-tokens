#!/bin/bash
# ============================================================
#  04-deploy-from-tmp.sh  —— 服务器端部署脚本
#
#  适用场景: 本地因权限问题无法直接 scp 到 /opt/，
#            先把产物传到服务器 /tmp/，再在服务器上执行本脚本。
#
#  前置:
#    /tmp/ai-token-backend.jar   后端 jar
#    /tmp/dist/                  前端构建产物（index.html 所在目录）
#
#  用法（在服务器上）:
#    sudo bash 04-deploy-from-tmp.sh              # 后端 + 前端
#    sudo bash 04-deploy-from-tmp.sh backend      # 仅后端
#    sudo bash 04-deploy-from-tmp.sh frontend     # 仅前端
# ============================================================

set -e

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }
step() { echo -e "\n${BLUE}──────── $* ────────${NC}"; }

# ── 配置 ──────────────────────────────────────────────────
DEPLOY_DIR="/opt/ai-token"
BACKEND_DIR="${DEPLOY_DIR}/backend"
FRONTEND_DIR="${DEPLOY_DIR}/frontend"     # nginx: root /opt/ai-token/frontend
DIST_LEGACY="${DEPLOY_DIR}/dist"          # 兼容旧 nginx 配置
LOG_DIR="${DEPLOY_DIR}/logs"
SERVICE_NAME="ai-token-backend"           # systemd 模板实例: ai-token-backend@8082
INSTANCES="8082 8083"

JAR_SRC="/tmp/ai-token-backend.jar"
DIST_SRC="/tmp/dist"

TARGET="${1:-all}"
case "$TARGET" in
    all|backend|frontend) ;;
    *) err "未知参数: $TARGET (可选: all / backend / frontend)" ;;
esac

[ "$(id -u)" -eq 0 ] || err "请使用 sudo 执行本脚本"

echo ""
log "=========================================="
log "  AI Token 部署（源: /tmp）  目标: ${TARGET}"
log "=========================================="

# ── 前置检查 ──────────────────────────────────────────────
if [ "$TARGET" = "all" ] || [ "$TARGET" = "backend" ]; then
    [ -f "$JAR_SRC" ] || err "JAR 不存在: ${JAR_SRC}"
    log "JAR:  ${JAR_SRC}  ($(du -h "$JAR_SRC" | cut -f1))"
fi
if [ "$TARGET" = "all" ] || [ "$TARGET" = "frontend" ]; then
    [ -d "$DIST_SRC" ] || err "前端目录不存在: ${DIST_SRC}"
    [ -f "${DIST_SRC}/index.html" ] || err "缺少 index.html，确认 ${DIST_SRC} 是构建产物根目录"
    log "dist: ${DIST_SRC}"
fi

# ══════════════════════════════════════════════════════════
#  后端部署
# ══════════════════════════════════════════════════════════
if [ "$TARGET" = "all" ] || [ "$TARGET" = "backend" ]; then

    step "停止服务"
    for PORT in $INSTANCES; do
        INST="${SERVICE_NAME}@${PORT}"
        if systemctl is-active --quiet "$INST" 2>/dev/null; then
            systemctl stop "$INST"
            log "已停止 ${INST}"
        fi
    done
    sleep 2

    step "归档旧日志"
    TS=$(date +%Y%m%d-%H%M%S)
    for PORT in $INSTANCES; do
        OLD="${LOG_DIR}/ai-token-${PORT}.log"
        if [ -f "$OLD" ] && [ -s "$OLD" ]; then
            ARCHIVE="${LOG_DIR}/ai-token-${PORT}.${TS}.log"
            mv "$OLD" "$ARCHIVE"
            chown ai-token:ai-token "$ARCHIVE"
            log "已归档: $(basename "$ARCHIVE")"
        fi
    done

    step "备份并替换 JAR"
    mkdir -p "${BACKEND_DIR}" "${LOG_DIR}"
    if [ -f "${BACKEND_DIR}/ai-token-backend.jar" ]; then
        BACKUP="${BACKEND_DIR}/ai-token-backend.jar.$(date +%Y%m%d%H%M%S).bak"
        mv "${BACKEND_DIR}/ai-token-backend.jar" "$BACKUP"
        log "旧 JAR 已备份: $(basename "$BACKUP")"
        # 只保留上一个版本（删除更早的备份，便于快速回滚）
        ls -t "${BACKEND_DIR}"/*.bak 2>/dev/null | tail -n +2 | xargs -r rm -f
    fi

    cp "$JAR_SRC" "${BACKEND_DIR}/ai-token-backend.jar"
    chown ai-token:ai-token "${BACKEND_DIR}/ai-token-backend.jar"
    chmod 644 "${BACKEND_DIR}/ai-token-backend.jar"
    log "新 JAR 已部署"

    step "启动服务"
    for PORT in $INSTANCES; do
        INST="${SERVICE_NAME}@${PORT}"
        systemctl start "$INST"
        log "已启动 ${INST}"
    done

    step "等待就绪"
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
        [ "$ok" -eq 0 ] && warn "实例 :${PORT} 启动超时，查看: journalctl -u ${SERVICE_NAME}@${PORT} -n 50"
    done
fi

# ══════════════════════════════════════════════════════════
#  前端部署
# ══════════════════════════════════════════════════════════
if [ "$TARGET" = "all" ] || [ "$TARGET" = "frontend" ]; then

    step "部署前端静态文件"

    deploy_frontend() {
        local DEST="$1"
        rm -rf "${DEST:?}"/*
        mkdir -p "${DEST}"
        cp -r "${DIST_SRC}/." "${DEST}/"
        chown -R www-data:www-data "${DEST}"
        find "${DEST}" -type d -exec chmod 755 {} \;
        find "${DEST}" -type f -exec chmod 644 {} \;
        log "已部署 → ${DEST}"
    }

    # nginx 实际配置: location /ai-token/ { root /opt/ai-token/frontend; }
    # 因此文件需放在 frontend/ai-token/ 下
    deploy_frontend "${FRONTEND_DIR}/ai-token"

    # 兼容旧配置（location 使用 /opt/ai-token/dist）
    if [ -d "$DIST_LEGACY" ]; then
        deploy_frontend "${DIST_LEGACY}/ai-token"
    fi

    step "重载 Nginx"
    nginx -t || err "Nginx 配置有误"
    systemctl reload nginx
    log "Nginx 已重载"
fi

# ── 摘要 ──────────────────────────────────────────────────
echo ""
log "=========================================="
log "  部署完成！ $(date '+%Y-%m-%d %H:%M:%S')"
log "  访问: http://ai.ainetlab.net/ai-token/"
if [ "$TARGET" = "all" ] || [ "$TARGET" = "backend" ]; then
    log "  状态: systemctl status ${SERVICE_NAME}@8082"
    log "  日志: tail -f ${LOG_DIR}/ai-token-8082.log"
fi
log "=========================================="
