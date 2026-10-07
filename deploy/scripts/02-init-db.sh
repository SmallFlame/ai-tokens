#!/bin/bash
# ============================================================
#  02-init-db.sh
#  功能: Ubuntu 20 + MySQL 8 数据库初始化
#       - 设置 root 密码（Ubuntu 默认 auth_socket 无密码）
#       - 创建应用专用用户与数据库
#       - 导入 init.sql
#  执行: sudo bash 02-init-db.sh
# ============================================================
set -e

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
log()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC}  $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# ── 配置项（按需修改）────────────────────────────────────
MYSQL_NEW_ROOT_PASS="Root@2026Ai!"        # MySQL root 新密码
APP_DB_USER="ai_token_user"               # 应用数据库用户名
APP_DB_PASS="AiToken@DB2026!"             # 应用数据库密码
APP_DB_NAME="ai_token"                    # 数据库名
# init.sql 路径（脚本相对路径）
INIT_SQL_PATH="$(cd "$(dirname "$0")/../.."; pwd)/sql/init.sql"

# 检查 MySQL 是否运行（Ubuntu 服务名为 mysql）
systemctl is-active --quiet mysql || err "MySQL 未运行，请先执行 01-install-env.sh"

log "检测 MySQL root 认证方式 ..."
# 优先尝试 auth_socket（无密码），再尝试已配置的密码，均失败则报错
if mysql -u root -e "SELECT 1" &>/dev/null 2>&1; then
    log "auth_socket 模式，正在设置 root 密码 ..."
    mysql -u root <<EOF
ALTER USER 'root'@'localhost'
    IDENTIFIED WITH mysql_native_password BY '${MYSQL_NEW_ROOT_PASS}';
DELETE FROM mysql.user WHERE User = '' OR (User = 'root' AND Host != 'localhost');
DROP DATABASE IF EXISTS test;
DELETE FROM mysql.db WHERE Db = 'test' OR Db = 'test\\_%';
FLUSH PRIVILEGES;
EOF
    log "root 密码已设置为: ${MYSQL_NEW_ROOT_PASS}"
elif mysql -u root -p"${MYSQL_NEW_ROOT_PASS}" -e "SELECT 1" &>/dev/null 2>&1; then
    log "root 密码已存在且正确，跳过 ALTER USER"
else
    err "无法连接 MySQL root，请确认密码是否为: ${MYSQL_NEW_ROOT_PASS}"
fi

log "创建应用数据库和用户 ..."
mysql -u root -p"${MYSQL_NEW_ROOT_PASS}" <<EOF
CREATE DATABASE IF NOT EXISTS \`${APP_DB_NAME}\`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS '${APP_DB_USER}'@'localhost'
    IDENTIFIED WITH mysql_native_password BY '${APP_DB_PASS}';

GRANT ALL PRIVILEGES ON \`${APP_DB_NAME}\`.* TO '${APP_DB_USER}'@'localhost';

FLUSH PRIVILEGES;
EOF
log "数据库 [${APP_DB_NAME}] 和用户 [${APP_DB_USER}] 创建完成"

# 导入建表脚本
if [ -f "$INIT_SQL_PATH" ]; then
    log "导入 init.sql: ${INIT_SQL_PATH}"
    mysql -u root -p"${MYSQL_NEW_ROOT_PASS}" "${APP_DB_NAME}" < "${INIT_SQL_PATH}"
    log "数据库初始化完成"
else
    warn "未找到 init.sql，路径: ${INIT_SQL_PATH}"
    warn "请手动执行: mysql -u root -p ${APP_DB_NAME} < /path/to/init.sql"
fi

# 将 root 密码同步到 .env（使用 root 用户连接，最简可靠）
ENV_FILE="/opt/ai-token/backend/.env"
if [ -f "$ENV_FILE" ]; then
    sed -i "s|^DB_USERNAME=.*|DB_USERNAME=root|" "$ENV_FILE"
    sed -i "s|^DB_PASSWORD=.*|DB_PASSWORD=${MYSQL_NEW_ROOT_PASS}|" "$ENV_FILE"
    log ".env 数据库配置已同步 (root)"
fi

echo ""
log "========================================"
log "  数据库初始化完成！"
log "  root 密码  : ${MYSQL_NEW_ROOT_PASS}"
log "  应用账号  : ${APP_DB_USER} / ${APP_DB_PASS}"
log "  下一步    : sudo bash 03-deploy.sh"
log "========================================"
