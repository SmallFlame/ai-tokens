#!/bin/bash
# ============================================================
#  AI Token 一键部署脚本（从本地开发机执行）
#
#  用法:
#    bash deploy/scripts/oneshot-deploy.sh                          # 全量(后端+前端)
#    bash deploy/scripts/oneshot-deploy.sh backend                  # 仅后端
#    bash deploy/scripts/oneshot-deploy.sh frontend                 # 仅前端
#    bash deploy/scripts/oneshot-deploy.sh --skip-build             # 跳过构建，用现有产物
#    bash deploy/scripts/oneshot-deploy.sh --migrate x.sql          # 执行数据库迁移脚本
#
#  前置: 本地已配置 ssh 免密登录到 $SERVER
# ============================================================

set -e

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }
step() { echo -e "\n${BLUE}════ $* ════${NC}"; }

# ── 配置 ──────────────────────────────────────────────────
SERVER="ubuntu@ai.ainetlab.net"
BACKEND_DIR="/opt/ai-token/backend"
FRONTEND_DIR="/opt/ai-token/frontend/ai-token"
SERVICE="ai-token-backend"
INSTANCES="8082 8083"
DB_NAME="ai_token"

# 项目根目录（脚本所在目录的上两级）
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
BACKEND_SRC="${ROOT_DIR}/ai-token-backend"
FRONTEND_SRC="${ROOT_DIR}/ai-token-frontend"

# ── 参数解析 ──────────────────────────────────────────────
TARGET=""
SKIP_BUILD=0
MIGRATE_FILE=""
while [ $# -gt 0 ]; do
    case "$1" in
        all|backend|frontend) TARGET="$1" ;;
        --skip-build) SKIP_BUILD=1 ;;
        --migrate)    shift; MIGRATE_FILE="$1" ;;
        *) err "未知参数: $1 (可选: all / backend / frontend / --skip-build / --migrate <文件名>)" ;;
    esac
    shift
done
[ -z "$TARGET" ] && TARGET="all"

echo ""
log "=========================================="
log "  AI Token 部署 → ${SERVER}"
log "  目标: ${TARGET}  跳过构建: ${SKIP_BUILD}"
log "=========================================="

# ── Step 1: 构建后端 ──────────────────────────────────────
if [ "$TARGET" = "all" ] || [ "$TARGET" = "backend" ]; then
    if [ "$SKIP_BUILD" -eq 0 ]; then
        step "构建后端 (mvn package)"
        cd "${BACKEND_SRC}"
        mvn clean package -DskipTests -q || err "后端构建失败"
        log "后端构建完成"
    fi
    JAR=$(ls -t "${BACKEND_SRC}"/target/ai-token-backend-*.jar 2>/dev/null | grep -v original | head -1)
    [ -f "$JAR" ] || err "未找到 JAR: ${BACKEND_SRC}/target/*.jar"
    log "JAR: $(basename "$JAR")  ($(du -h "$JAR" | cut -f1))"
fi

# ── Step 2: 构建前端 ──────────────────────────────────────
if [ "$TARGET" = "all" ] || [ "$TARGET" = "frontend" ]; then
    if [ "$SKIP_BUILD" -eq 0 ]; then
        step "构建前端 (npm run build)"
        cd "${FRONTEND_SRC}"
        npx vue-cli-service build || err "前端构建失败"
        log "前端构建完成"
    fi
    [ -d "${FRONTEND_SRC}/dist" ] || err "未找到 dist: ${FRONTEND_SRC}/dist"
fi

# ── Step 3: 上传产物 ──────────────────────────────────────
step "上传产物到服务器 /tmp"

if [ "$TARGET" = "all" ] || [ "$TARGET" = "backend" ]; then
    scp "$JAR" "${SERVER}:/tmp/ai-token-backend.jar" || err "JAR 上传失败"
    log "JAR 已上传"
fi

if [ "$TARGET" = "all" ] || [ "$TARGET" = "frontend" ]; then
    ssh "$SERVER" "rm -rf /tmp/dist && mkdir -p /tmp/dist"
    scp -r "${FRONTEND_SRC}/dist/." "${SERVER}:/tmp/dist/" || err "前端上传失败"
    log "前端已上传"
fi

# ── Step 4: 数据库迁移（可选，需显式指定 --migrate <脚本名>）──
# 注意：不自动执行所有 migrate_*.sql，因为历史脚本不幂等，重复执行会报错。
if [ -n "$MIGRATE_FILE" ]; then
    step "执行数据库迁移: ${MIGRATE_FILE}"
    [ -f "${BACKEND_SRC}/sql/${MIGRATE_FILE}" ] || err "迁移脚本不存在: sql/${MIGRATE_FILE}"
    scp "${BACKEND_SRC}/sql/${MIGRATE_FILE}" "${SERVER}:/tmp/${MIGRATE_FILE}"
    ssh "$SERVER" "sudo bash -c '
        set -a; source ${BACKEND_DIR}/.env 2>/dev/null; set +a
        mysql -u\"\$DB_USERNAME\" -p\"\$DB_PASSWORD\" \"\$DB_NAME\" < /tmp/${MIGRATE_FILE} \
          && echo \"  ✓ ${MIGRATE_FILE} 执行成功\"
    '" || err "迁移执行失败: ${MIGRATE_FILE}"
else
    # 列出可用的迁移脚本供参考
    PENDING=$(ls "${BACKEND_SRC}"/sql/migrate_*.sql 2>/dev/null || true)
    if [ -n "$PENDING" ]; then
        step "数据库迁移（未执行，如需执行请加 --migrate <文件名>）"
        for f in $PENDING; do echo "    bash $0 --migrate $(basename "$f")"; done
    fi
fi

# ── Step 5: 重启后端 ──────────────────────────────────────
if [ "$TARGET" = "all" ] || [ "$TARGET" = "backend" ]; then
    step "部署后端 (停服 → 备份 → 替换 → 启动)"
    ssh "$SERVER" "sudo bash -c '
        set -e
        # 停止所有实例
        for PORT in ${INSTANCES}; do
            systemctl stop ${SERVICE}@\$PORT 2>/dev/null || true
        done
        sleep 2

        # 备份旧 JAR（只保留最近 3 个）
        if [ -f ${BACKEND_DIR}/ai-token-backend.jar ]; then
            mv ${BACKEND_DIR}/ai-token-backend.jar \
               ${BACKEND_DIR}/ai-token-backend.jar.\$(date +%Y%m%d%H%M%S).bak
            ls -t ${BACKEND_DIR}/*.bak 2>/dev/null | tail -n +4 | xargs -r rm -f
        fi

        # 替换
        cp /tmp/ai-token-backend.jar ${BACKEND_DIR}/ai-token-backend.jar
        chown ai-token:ai-token ${BACKEND_DIR}/ai-token-backend.jar

        # 启动
        for PORT in ${INSTANCES}; do
            systemctl start ${SERVICE}@\$PORT
            echo \"  已启动 ${SERVICE}@\$PORT\"
        done
    '"
    log "后端已重启"
fi

# ── Step 6: 部署前端 ──────────────────────────────────────
if [ "$TARGET" = "all" ] || [ "$TARGET" = "frontend" ]; then
    step "部署前端静态文件"
    ssh "$SERVER" "sudo bash -c '
        set -e
        rm -rf ${FRONTEND_DIR:?}/*
        mkdir -p ${FRONTEND_DIR}
        cp -r /tmp/dist/. ${FRONTEND_DIR}/
        chown -R www-data:www-data ${FRONTEND_DIR}
        find ${FRONTEND_DIR} -type d -exec chmod 755 {} \;
        find ${FRONTEND_DIR} -type f -exec chmod 644 {} \;
    '"
    log "前端已部署"

    step "重载 Nginx"
    ssh "$SERVER" "sudo nginx -t && sudo systemctl reload nginx" || warn "Nginx 重载失败"
fi

# ── Step 7: 健康检查 ──────────────────────────────────────
if [ "$TARGET" = "all" ] || [ "$TARGET" = "backend" ]; then
    step "等待后端就绪"
    for PORT in $INSTANCES; do
        ok=0
        for i in $(seq 1 12); do
            sleep 5
            if ssh "$SERVER" "curl -sf http://127.0.0.1:${PORT}/api/auth/login \
                    -X POST -H 'Content-Type: application/json' -d '{}' &>/dev/null"; then
                log "实例 :${PORT} 已就绪 ✓"
                ok=1
                break
            fi
        done
        [ "$ok" -eq 0 ] && warn "实例 :${PORT} 启动超时，请查看: journalctl -u ${SERVICE}@${PORT} -n 50"
    done
fi

# ── 完成 ──────────────────────────────────────────────────
echo ""
log "=========================================="
log "  部署完成！"
log "  访问: http://ai.ainetlab.net/ai-token/"
log ""
log "  状态: ssh ${SERVER} 'systemctl status ${SERVICE}@8082'"
log "  日志: ssh ${SERVER} 'tail -f /opt/ai-token/logs/ai-token-8082.log'"
log "=========================================="
