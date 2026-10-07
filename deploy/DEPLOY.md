# AI Token 管理平台 - 部署手册（Ubuntu 20.04）

## 部署架构

```
用户浏览器
    │ HTTP :80
    ▼
┌─────────────────────────────────┐
│  Nginx                          │  ← 静态文件 + 反向代理 + 负载均衡
│  /          → dist/             │
│  /api/      → upstream          │
│  /v1/       → upstream          │（SSE 流式，关闭缓冲）
└──────┬──────────────┬───────────┘
       │ least_conn   │
       ▼              ▼
┌────────────┐  ┌────────────┐
│ Spring Boot│  │ Spring Boot│  ← 两个实例，systemd 守护
│  :8082     │  │  :8083     │
└─────┬──────┘  └─────┬──────┘
      │               │
      └───────┬────────┘
              │
       ┌──────┴──────┐
  MySQL:3306     Redis:6379
                 （Sa-Token Session 共享）
```

> Session 由 `sa-token-redis-jackson` 存储在 Redis，两个实例共享，无需 `ip_hash`。

---

## 第一步：本地构建（Windows）

### 1.1 构建前端

```powershell
cd ai-token-frontend
npm install
npm run build
# 产物: ai-token-frontend/dist/
```

### 1.2 构建后端

```powershell
cd ai-token-backend
mvn clean package -DskipTests
# 产物: ai-token-backend/target/ai-token-backend-1.0.0.jar
```

### 1.3 上传文件到服务器

使用 SCP / WinSCP / SFTP 上传以下文件到 Ubuntu 服务器：

| 本地文件 | 上传到服务器 |
|----------|-------------|
| `ai-token-frontend/dist/`（整个目录） | `/tmp/dist/` |
| `ai-token-backend/target/ai-token-backend-1.0.0.jar` | `/tmp/ai-token-backend.jar` |
| `sql/init.sql` | `/tmp/init.sql`（脚本会自动引用） |
| `deploy/`（整个目录） | `/tmp/deploy/` |

**SCP 示例（Git Bash / PowerShell）：**
```bash
scp -r ai-token-frontend/dist                         ubuntu@<服务器IP>:/tmp/dist
scp    ai-token-backend/target/ai-token-backend-*.jar ubuntu@<服务器IP>:/tmp/ai-token-backend.jar
scp    sql/init.sql                                   ubuntu@<服务器IP>:/tmp/init.sql
scp -r deploy                                         ubuntu@<服务器IP>:/tmp/deploy
```

---

## 第二步：服务器环境安装（Ubuntu 20.04）

SSH 登录服务器后执行：

```bash
# 赋予脚本执行权限
chmod +x /tmp/deploy/scripts/*.sh

# Step 1: 安装 JDK8 + MySQL8 + Redis + Nginx（约 5-10 分钟）
sudo bash /tmp/deploy/scripts/01-install-env.sh
```

**脚本自动完成：**
- `apt-get install openjdk-8-jdk`
- 从 MySQL 官方 APT 源安装 MySQL 8
- `apt-get install redis-server nginx`
- 创建 `/opt/ai-token/{frontend,backend,logs}` 目录
- 创建 `ai-token` 系统用户
- UFW 防火墙开放 22 / 80 / 443 端口

---

## 第三步：初始化数据库

```bash
sudo bash /tmp/deploy/scripts/02-init-db.sh
```

**脚本自动完成：**
- 为 root 设置密码（默认 `Root@2026Ai!`，可在脚本顶部修改）
- 创建应用用户 `ai_token_user` / `AiToken@DB2026!`
- 创建数据库 `ai_token`
- 导入 `sql/init.sql` 建表脚本

> Ubuntu 20 + MySQL 8 默认使用 `auth_socket`，root 登录无需密码，脚本利用此特性完成初始化后再设置密码。

---

## 第四步：部署应用

```bash
sudo bash /tmp/deploy/scripts/03-deploy.sh \
  /tmp/ai-token-backend.jar \
  /tmp/dist
```

**脚本自动完成：**
1. 首次部署：从模板创建 `/opt/ai-token/backend/.env`，提示填写密码
2. 备份旧 JAR（保留最近 3 个）
3. 部署 JAR → `/opt/ai-token/backend/`
4. 部署前端 → `/opt/ai-token/frontend/`（所有者设为 `www-data`）
5. 禁用 Nginx 默认站点，载入 `ai-token.conf`（含双实例 upstream）
6. 注册 systemd 模板单元，启动 `:8082` 和 `:8083` 两个实例
7. 分别等待两个实例就绪并打印访问地址

### 4.1 首次部署：检查 .env

脚本首次运行时会提示编辑 `.env`，确认密码与第三步一致：

```bash
sudo nano /opt/ai-token/backend/.env
```

```ini
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=ai_token
DB_USERNAME=ai_token_user
DB_PASSWORD=AiToken@DB2026!   # ← 与 02-init-db.sh 中一致

REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=

AES_KEY=AiToken@2026!Key      # ← 首次初始化后不可更改
```

---

## 第五步：验证访问

```bash
# 查看两个实例状态
systemctl status ai-token-backend@8082
systemctl status ai-token-backend@8083

# 分别测试两个实例直连
curl -s http://127.0.0.1:8082/api/auth/login \
  -X POST -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | python3 -m json.tool

curl -s http://127.0.0.1:8083/api/auth/login \
  -X POST -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | python3 -m json.tool

# 通过 Nginx 测试（走负载均衡）
curl -s http://127.0.0.1/api/auth/login \
  -X POST -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | python3 -m json.tool
```

浏览器访问：`http://<服务器公网IP>`

默认账号：`admin` / `admin123`（登录后请立即修改密码）

---

## 日常运维命令

```bash
# ── 服务管理（两个实例）────────────────────────────────
systemctl start   ai-token-backend@8082 ai-token-backend@8083
systemctl stop    ai-token-backend@8082 ai-token-backend@8083
systemctl restart ai-token-backend@8082 ai-token-backend@8083
systemctl status  ai-token-backend@8082
systemctl status  ai-token-backend@8083

# ── 日志 ────────────────────────────────────────────────
# 实例1 日志
journalctl -u ai-token-backend@8082 -n 100 --no-pager
journalctl -u ai-token-backend@8082 -f
tail -f /opt/ai-token/logs/ai-token-8082.log

# 实例2 日志
journalctl -u ai-token-backend@8083 -f
tail -f /opt/ai-token/logs/ai-token-8083.log

# ── Nginx ────────────────────────────────────────────────
nginx -t                   # 测试配置
systemctl reload nginx     # 热重载
systemctl restart nginx    # 重启

# ── 查看端口（确认两个实例均在监听）────────────────────
ss -tlnp | grep -E '80|8082|8083|3306|6379'
```

---

## 版本更新

### 常规代码更新（无 SQL 变更）

```bash
# 1. 本地构建
cd ai-token-frontend && npm run build
cd ../ai-token-backend && mvn clean package -DskipTests

# 2. 上传到服务器
scp ai-token-backend/target/*.jar  ubuntu@<IP>:/tmp/ai-token-backend.jar
scp -r ai-token-frontend/dist      ubuntu@<IP>:/tmp/dist

# 3. 部署（脚本会依次：停两个实例 → 替换 JAR/前端 → 启两个实例）
sudo bash /tmp/deploy/scripts/03-deploy.sh \
  /tmp/ai-token-backend.jar \
  /tmp/dist
```

### 本次更新（v2，含 SQL 结构变更）

> 适用于已运行过 `init.sql` 初始化、尚未执行 v2 迁移的数据库。

**第一步：备份数据库**

```bash
mysqldump -u ai_token_user -p'AiToken@DB2026!' ai_token \
  > ~/backup_$(date +%Y%m%d_%H%M%S).sql
```

**第二步：上传并执行迁移脚本**

```bash
# 在本地（Windows）将脚本上传到服务器
scp sql/migrate_v2.sql ubuntu@<IP>:/tmp/migrate_v2.sql

# 在服务器执行
mysql -u ai_token_user -p'AiToken@DB2026!' ai_token < /tmp/migrate_v2.sql
# 或使用 root（权限更高，CREATE TABLE 更稳妥）
mysql -u root -p ai_token < /tmp/migrate_v2.sql
```

> 若已单独执行过旧版 `migrate_role_hierarchy.sql`，则 **只需执行步骤 4 和 5**（t_model 和 t_group 部分）：
> ```bash
> mysql -u root -p ai_token <<'EOF'
> ALTER TABLE `t_model`
>   ADD COLUMN `cache_creation_price` DECIMAL(10,6) DEFAULT NULL
>     COMMENT '缓存创建单价（元/千Token），NULL=等于输入单价',
>   ADD COLUMN `cache_read_price`     DECIMAL(10,6) DEFAULT NULL
>     COMMENT '缓存命中单价（元/千Token），NULL=输入单价×0.1';
>
> CREATE TABLE IF NOT EXISTS `t_group` (
>   `id`         BIGINT       NOT NULL AUTO_INCREMENT,
>   `name`       VARCHAR(100) NOT NULL,
>   `remark`     VARCHAR(500) DEFAULT NULL,
>   `leader_id`  BIGINT       DEFAULT NULL,
>   `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,
>   PRIMARY KEY (`id`),
>   INDEX `idx_leader_id` (`leader_id`)
> ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小组';
> EOF
> ```

**第三步：部署新代码**

```bash
# 本地构建
cd ai-token-frontend && npm run build
cd ../ai-token-backend && mvn clean package -DskipTests

# 上传
scp ai-token-backend/target/*.jar  ubuntu@<IP>:/tmp/ai-token-backend.jar
scp -r ai-token-frontend/dist      ubuntu@<IP>:/tmp/dist

# 部署（自动重启两个实例）
sudo bash /tmp/deploy/scripts/03-deploy.sh \
  /tmp/ai-token-backend.jar \
  /tmp/dist
```

**第四步：验证**

```bash
# 确认新字段存在
mysql -u ai_token_user -p'AiToken@DB2026!' ai_token -e "
  SHOW COLUMNS FROM t_user LIKE 'group_id';
  SHOW COLUMNS FROM t_model LIKE 'cache_creation_price';
  SHOW TABLES LIKE 't_group';
"

# 确认两个实例正常
systemctl status ai-token-backend@8082 ai-token-backend@8083

# 通过 Nginx 验证接口
curl -s http://127.0.0.1/api/auth/login \
  -X POST -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | python3 -m json.tool
```

---

## 常见问题

### Q1: 后端启动失败 - 数据库连接错误
```bash
# 检查 MySQL 服务（Ubuntu 服务名为 mysql，非 mysqld）
systemctl status mysql
# 验证连接
mysql -u ai_token_user -p'AiToken@DB2026!' ai_token -e "SELECT 1"
# 检查 .env
sudo cat /opt/ai-token/backend/.env
```

### Q2: 后端启动失败 - Redis 连接失败
```bash
systemctl status redis-server
redis-cli ping   # 应返回 PONG
```

### Q3: Nginx 403 Forbidden
```bash
# Ubuntu Nginx 用户是 www-data，确认文件权限
ls -la /opt/ai-token/frontend/
# 修复
sudo chown -R www-data:www-data /opt/ai-token/frontend
```

### Q4: 前端页面 404（刷新后）
确认 Nginx 配置中有 `try_files $uri $uri/ /index.html`：
```bash
grep -A2 "location /" /etc/nginx/conf.d/ai-token.conf
```

### Q5: MySQL 8 密码策略报错
```sql
-- 临时降低（不建议生产）
SET GLOBAL validate_password.policy = LOW;
SET GLOBAL validate_password.length = 6;
```

### Q6: Java 版本不对（多版本共存）
```bash
sudo update-alternatives --config java
# 选择 java-8 对应的序号
```

### Q7: 某个实例挂掉，另一个还在跑
```bash
# Nginx 的 least_conn 会自动跳过无响应的实例
# 检查并重启挂掉的实例
systemctl status ai-token-backend@8083
journalctl -u ai-token-backend@8083 -n 50
systemctl restart ai-token-backend@8083
```

### Q8: 登录后跳回登录页（Session 失效）
Sa-Token session 存在 Redis，两个实例共享同一套 session，通常不会出现此问题。如果出现：
```bash
# 检查 Redis 是否正常
redis-cli ping
redis-cli keys "satoken*" | head -5
# 检查两个实例是否连接同一个 Redis
grep REDIS /opt/ai-token/backend/.env
```

---

## 目录结构

```
/opt/ai-token/
├── frontend/                   ← Vue 构建产物（Nginx 根目录，所有者 www-data）
├── backend/
│   ├── ai-token-backend.jar
│   ├── application-prod.yml
│   └── .env                    ← chmod 600，仅 ai-token 用户可读
└── logs/
    ├── ai-token-8082.log        ← 实例1 日志
    ├── ai-token-8083.log        ← 实例2 日志
    ├── heap-dump-8082.hprof     ← OOM 时自动生成
    └── heap-dump-8083.hprof

/etc/nginx/conf.d/
    └── ai-token.conf

/etc/systemd/system/
    └── ai-token-backend@.service   ← 模板单元（%i = 端口号）
```
