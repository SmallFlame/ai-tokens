# AI Token 管理平台

一个轻量级的 AI 接口代理与 Token 用量管理平台，支持接入任意 OpenAI 兼容的第三方大模型接口，统一管理 API Key、记录每次调用的 Token 消耗与费用，并提供管理后台和用户门户两套界面。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 2.7 + JDK 8 |
| 持久层 | MyBatis-Plus + MySQL 8 |
| 缓存 / 会话 | Redis + Sa-Token |
| 代理转发 | OkHttp 4（支持 SSE 流式透传） |
| 前端 | Vue 2 + Element UI + ECharts 5 |
| 部署 | Nginx + systemd（Ubuntu 20.04） |

---

## 系统架构

```
用户 / OpenAI SDK
      │  HTTP :80
      ▼
   Nginx
   /        → 前端静态文件
   /api/     → Spring Boot :8082
   /v1/      → Spring Boot :8082（SSE 关闭缓冲）
      │
 Spring Boot
   ├── 代理层：验证平台 Key → 选渠道 → 转发至第三方 AI
   ├── 统计层：异步记录 Token 用量、费用
   └── 管理层：渠道 / 模型 / 用户 / API Key 管理
      │
  MySQL        Redis
  业务数据      Sa-Token 会话
```

---

## 功能模块

### 管理后台（管理员）

| 模块 | 说明 |
|------|------|
| 数据看板 | 调用量趋势图（折线+柱状双轴）、模型分布饼图、今日汇总 |
| 渠道管理 | 新增/编辑/删除 AI 接口渠道，配置 Base URL、API Key（AES 加密存储）、超时时间 |
| 模型管理 | 维护模型列表，配置输入/输出 Token 单价，支持模型别名映射 |
| API Key 管理 | 为用户分发平台 Key，支持额度限制与吊销 |
| 调用日志 | 全量日志查询，按用户/渠道/模型/时间/状态筛选 |
| 用户管理 | 查看所有用户、启用/禁用/删除账号 |

### 用户门户（普通用户）

| 模块 | 说明 |
|------|------|
| 我的概览 | 今日与累计的调用次数、Token 消耗、费用 |
| 我的密钥 | 查看/创建/吊销自己的 API Key |
| 调用记录 | 查询自己的历史调用明细 |
| 账号管理 | 管理员可创建新账号并分发给同学/同事（普通用户不可见） |
| 修改密码 | 修改自己的登录密码 |

### 代理转发

- 兼容 OpenAI SDK 格式，直接替换 `base_url` 即可接入
- 支持流式（SSE）和非流式两种响应
- 自动解析 `usage` 字段，记录 Prompt / Completion Tokens
- 费用按模型单价实时计算（精确到小数点后 6 位）

---

## 项目结构

```
ai-token/
├── ai-token-backend/
│   └── src/main/java/com/aitoken/
│       ├── controller/        # REST 接口（Auth/My/User/ApiKey/Channel/Model/Log/Stat/Proxy）
│       ├── service/           # 业务逻辑
│       ├── mapper/            # MyBatis-Plus Mapper
│       ├── entity/            # 数据库实体
│       ├── dto/               # 请求/响应 DTO
│       ├── config/            # Sa-Token、MyBatis-Plus、CORS 配置
│       └── util/              # AES 加密、API Key 生成
├── ai-token-frontend/
│   └── src/
│       ├── layout/            # 管理后台布局 / 用户门户布局
│       ├── views/
│       │   ├── dashboard/     # 管理看板
│       │   ├── channel/       # 渠道管理
│       │   ├── model/         # 模型管理
│       │   ├── apikey/        # API Key 管理
│       │   ├── log/           # 调用日志（管理员）
│       │   ├── user/          # 用户管理
│       │   └── portal/        # 用户门户（概览/密钥/记录/账号/改密）
│       ├── api/               # Axios 请求模块
│       ├── store/             # Vuex
│       └── router/            # Vue Router（按角色分流）
├── sql/
│   └── init.sql               # 7 张表的建表与初始化数据
├── deploy/
│   ├── nginx/                 # Nginx 配置
│   ├── systemd/               # systemd 服务文件
│   ├── config/                # application-prod.yml / .env.example
│   ├── scripts/               # 一键部署脚本（01 装环境 / 02 初始化DB / 03 部署）
│   └── DEPLOY.md              # 部署手册
└── README.md
```

---

## 数据库表

| 表名 | 说明 |
|------|------|
| `t_user` | 平台用户（role: 0=管理员, 1=普通用户） |
| `t_api_key` | 用户 API Key（密钥值明文存储，渠道 Key AES 加密） |
| `t_channel` | AI 接口渠道 |
| `t_model` | 模型配置与 Token 单价 |
| `t_call_log` | 调用明细日志 |
| `t_token_stat` | Token 聚合统计（按天按模型汇总） |
| `t_sys_config` | 系统参数 |

---

## 快速启动（本地开发）

**前置依赖：** JDK 8、Maven 3.6+、MySQL 8、Redis、Node.js 14+

```bash
# 1. 初始化数据库
mysql -u root -p < sql/init.sql

# 2. 启动后端（修改 application.yml 中的数据库密码）
cd ai-token-backend
mvn spring-boot:run

# 3. 启动前端
cd ai-token-frontend
npm install
npm run serve
```

访问 `http://localhost:3000`，默认账号：`admin / admin123`

- 管理员登录后进入**管理后台**
- 普通用户登录后进入**用户门户**

---

## 代理接口使用

与 OpenAI SDK 完全兼容，替换 `base_url` 即可：

```python
from openai import OpenAI

client = OpenAI(
    api_key="sk-xxxxxxxx",          # 平台分发的 API Key
    base_url="http://<服务器IP>/v1"  # 替换为平台地址
)

response = client.chat.completions.create(
    model="gpt-4o",
    messages=[{"role": "user", "content": "你好"}]
)
```

---

## 服务器部署

详见 [deploy/DEPLOY.md](deploy/DEPLOY.md)，三步完成 Ubuntu 20.04 部署：

```bash
sudo bash /tmp/deploy/scripts/01-install-env.sh  # 安装 JDK/MySQL/Redis/Nginx
sudo bash /tmp/deploy/scripts/02-init-db.sh       # 初始化数据库
sudo bash /tmp/deploy/scripts/03-deploy.sh        # 部署应用
```

---

## 使用

● 使用步骤

  第一步：在用户门户创建密钥

  1. 登录后进入 我的密钥 页面
  2. 点击「新建密钥」，填写名称（如 claude-code），点击创建
  3. 复制生成的 sk-xxxxxxxx 格式密钥，只显示一次，务必保存

---
  第二步：在模型管理中确认模型已配置

  管理员需要先完成：
  - 渠道管理 → 新增渠道（类型 anthropic，Base URL 填 https://code.newcli.com/claude，填入真实 Anthropic Key）
  - 模型管理 → 新增模型，选择上面的渠道，模型名填 claude-sonnet-4-6（或其他 Claude 模型）

  这步如果没做，调用会报「模型未配置」。

---
  第三步：配置 Claude Code

  找到 Claude Code 的配置文件，路径通常是：

  ~/.claude.json          # Linux / macOS
  C:\Users\你的用户名\.claude.json   # Windows

  修改内容如下：

  {
    "env": {
      "ANTHROPIC_AUTH_TOKEN": "sk-你从平台复制的密钥",
      "ANTHROPIC_BASE_URL": "http://你的服务器地址:8082",
      "CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC": 1
    }
  }

  本地开发时 Base URL 填：
  http://localhost:8082

  生产部署后 填你的域名或服务器 IP：
  http://your-server.com

  注意：Base URL 不要加 /v1，代理层会自动拼接。

---
  第四步：验证是否生效

  重启 Claude Code 后，随便发一条消息，然后去平台的 调用日志 页面，能看到这条请求记录说明配置成功。

  如果有错误可以看日志里的 错误信息 列排查。

---
  常见问题

  ┌────────────────────────┬────────────────────────────────────────────┐
  │          现象          │                    原因                    │
  ├────────────────────────┼────────────────────────────────────────────┤
  │ 401 Missing x-api-key  │ ANTHROPIC_AUTH_TOKEN 没配置或写错了        │
  ├────────────────────────┼────────────────────────────────────────────┤
  │ 400 模型未配置         │ 管理员还没在模型管理里添加对应模型         │
  ├────────────────────────┼────────────────────────────────────────────┤
  │ 503 模型对应渠道不可用 │ 渠道被禁用或健康检测失败，去渠道管理里检查 │
  ├────────────────────────┼────────────────────────────────────────────┤
  │ 请求没有响应           │ ANTHROPIC_BASE_URL 地址不对或服务没启动    │
  └────────────────────────┴────────────────────────────────────────────┘


## License

MIT
