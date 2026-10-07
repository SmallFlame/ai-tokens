#!/bin/bash
# ============================================================
#  01-install-env.sh
#  功能: Ubuntu 20.04 一键安装 JDK8 / MySQL8 / Redis / Nginx
#  执行: sudo bash 01-install-env.sh
# ============================================================
set -e

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
log()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC}  $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# 检查系统
[ "$(id -u)" -eq 0 ] || err "请使用 root 或 sudo 执行此脚本"
grep -qi "ubuntu" /etc/os-release || warn "当前系统非 Ubuntu，脚本可能不兼容"
UBUNTU_VER=$(lsb_release -rs 2>/dev/null || echo "20.04")
log "系统: Ubuntu ${UBUNTU_VER}"

# 更新包索引
log "更新 apt 包索引 ..."
apt-get update -qq

# ── 安装 JDK 8 ───────────────────────────────────────────
install_jdk() {
    if java -version 2>&1 | grep -q '1\.8'; then
        warn "JDK 8 已安装，跳过"
        return
    fi
    log "安装 OpenJDK 8 ..."
    apt-get install -y openjdk-8-jdk
    # 若系统有多个 JDK，确保默认使用 Java 8
    update-alternatives --set java \
        "$(update-alternatives --list java | grep 'java-8')" 2>/dev/null || true
    java -version
    log "JDK 8 安装完成"
}

# ── 安装 MySQL 8 ─────────────────────────────────────────
# 直接使用 Ubuntu 官方源（Ubuntu 20.04/22.04 官方仓库自带 MySQL 8.0）
# 避免 MySQL 官方 APT 源 GPG 密钥过期问题
install_mysql() {
    if systemctl is-active --quiet mysql 2>/dev/null; then
        warn "MySQL 已在运行，跳过安装"
        return
    fi
    log "安装 MySQL 8（Ubuntu 官方源）..."

    # 清理残留的 MySQL 官方 APT 源配置（若之前已添加）
    rm -f /etc/apt/sources.list.d/mysql.list \
          /usr/share/keyrings/mysql.gpg \
          /tmp/mysql-apt-config.deb 2>/dev/null || true
    apt-get update -qq

    # Ubuntu 20.04 (focal) 和 22.04 (jammy) 官方源均提供 MySQL 8.0
    DEBIAN_FRONTEND=noninteractive apt-get install -y mysql-server

    systemctl enable mysql
    systemctl start  mysql

    MYSQL_VER=$(mysql --version | awk '{print $3}')
    log "MySQL ${MYSQL_VER} 安装完成"
}

# ── 安装 Redis ───────────────────────────────────────────
install_redis() {
    if systemctl is-active --quiet redis-server 2>/dev/null; then
        warn "Redis 已在运行，跳过安装"
        return
    fi
    log "安装 Redis ..."
    apt-get install -y redis-server

    # 绑定本地，关闭外网访问
    sed -i 's/^bind .*/bind 127.0.0.1/' /etc/redis/redis.conf

    # Ubuntu 20 默认 supervised 为 no，需改为 systemd
    sed -i 's/^supervised no/supervised systemd/' /etc/redis/redis.conf

    systemctl enable redis-server
    systemctl restart redis-server
    redis-cli ping && log "Redis 安装完成" || err "Redis 启动失败"
}

# ── 安装 Nginx ───────────────────────────────────────────
install_nginx() {
    if systemctl is-active --quiet nginx 2>/dev/null; then
        warn "Nginx 已在运行，跳过安装"
        return
    fi
    log "安装 Nginx ..."
    apt-get install -y nginx
    systemctl enable nginx
    systemctl start  nginx
    log "Nginx 安装完成，版本: $(nginx -v 2>&1)"
}

# ── 创建目录和系统用户 ────────────────────────────────────
setup_dirs() {
    log "创建目录结构 ..."
    # Ubuntu 下 adduser --system 更规范
    id ai-token &>/dev/null || adduser --system --no-create-home --group ai-token
    mkdir -p /opt/ai-token/{frontend,backend,logs}
    chown -R ai-token:ai-token /opt/ai-token
    chmod 755 /opt/ai-token
    log "目录创建完成: /opt/ai-token/{frontend,backend,logs}"
}

# ── 配置防火墙 UFW ────────────────────────────────────────
setup_firewall() {
    log "配置 UFW 防火墙 ..."
    if ! command -v ufw &>/dev/null; then
        apt-get install -y ufw
    fi
    ufw allow OpenSSH
    ufw allow 80/tcp
    ufw allow 443/tcp
    # 非交互启用（若已启用则仅更新规则）
    ufw --force enable
    ufw status
    log "防火墙已开放 SSH / HTTP / HTTPS"
}

# ── 主流程 ───────────────────────────────────────────────
install_jdk
install_mysql
install_redis
install_nginx
setup_dirs
setup_firewall

echo ""
log "========================================"
log "  环境安装完成！"
log "  下一步: sudo bash 02-init-db.sh"
log "========================================"
