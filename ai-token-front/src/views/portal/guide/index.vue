<template>
  <div class="guide-wrap">
    <div class="page-title">使用指南</div>

    <!-- 步骤时间线 -->
    <el-steps :active="5" direction="vertical" class="steps">

      <!-- Step 1 -->
      <el-step title="安装 Node.js（版本 ≥ 20）">
        <div slot="description" class="step-body">
          <p>Claude Code 基于 Node.js 运行，请先确认已安装合适的版本：</p>
          <div class="code-block"><code>node -v</code></div>
          <p>如未安装或版本过低，前往官网下载：</p>
          <a href="https://nodejs.org" target="_blank" class="link">
            <i class="el-icon-link" /> nodejs.org
          </a>
        </div>
      </el-step>

      <!-- Step 2 -->
      <el-step title="全局安装 Claude Code">
        <div slot="description" class="step-body">
          <p>打开终端（Windows 用 PowerShell 或 CMD，macOS/Linux 用 Terminal），执行：</p>
          <div class="code-block">
            <code>npm install -g @anthropic-ai/claude-code</code>
            <el-button type="text" class="copy-btn" icon="el-icon-copy-document"
              @click="copy('npm install -g @anthropic-ai/claude-code')" />
          </div>
          <p>安装完成后验证：</p>
          <div class="code-block"><code>claude --version</code></div>
        </div>
      </el-step>

      <!-- Step 3 -->
      <el-step title="获取你的 API Key">
        <div slot="description" class="step-body">
          <p>
            前往
            <router-link to="/portal/mykeys" class="link">
              <i class="el-icon-key" /> 我的密钥
            </router-link>
            页面，点击「新建密钥」，复制生成的 Key（格式为 <code class="inline">sk-xxx...</code>）。
          </p>
          <el-alert type="warning" show-icon :closable="false" style="margin-top:10px">
            <span slot="title">Key 仅在创建时完整显示，请妥善保存</span>
          </el-alert>
        </div>
      </el-step>

      <!-- Step 4 -->
      <el-step title="配置 API 地址和密钥">
        <div slot="description" class="step-body">
          <p>选择任一方式完成配置，<strong>推荐使用配置文件</strong>，一次配置永久生效。</p>

          <el-tabs v-model="cfgTab" type="card" class="os-tabs">

            <!-- 推荐：settings.json -->
            <el-tab-pane label="✅ 推荐：编辑配置文件" name="json">
              <p>
                用文本编辑器打开（如果文件不存在则新建）：
              </p>
              <div class="code-block">
                <code>C:\Users\你的用户名\.claude\settings.json</code>
                <el-button type="text" class="copy-btn" icon="el-icon-copy-document"
                  @click="copy('C:\\Users\\你的用户名\\.claude\\settings.json')" />
              </div>
              <p>将以下内容粘贴进去，把 <code class="inline">sk-你的密钥</code> 替换为「我的密钥」页面复制的 Key：</p>
              <div class="code-block">
                <code>{{ settingsJson }}</code>
                <el-button type="text" class="copy-btn" icon="el-icon-copy-document"
                  @click="copy(settingsJson)" />
              </div>
              <p class="tip">
                macOS / Linux 对应路径为 <code class="inline">~/.claude/settings.json</code>
              </p>
              <el-alert type="info" show-icon :closable="false" style="margin-top:10px">
                <div slot="title">
                  <code class="inline">CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC: 1</code>
                  可防止 Claude Code 向官方服务器发送遥测数据，避免因无法访问导致的卡顿。
                </div>
              </el-alert>
            </el-tab-pane>

            <!-- macOS / Linux 环境变量 -->
            <el-tab-pane label="macOS / Linux 环境变量" name="unix">
              <p>在 <code class="inline">~/.bashrc</code> 或 <code class="inline">~/.zshrc</code> 末尾添加：</p>
              <div class="code-block">
                <code>export ANTHROPIC_BASE_URL={{ baseUrl }}<br>export ANTHROPIC_AUTH_TOKEN=sk-你的密钥</code>
                <el-button type="text" class="copy-btn" icon="el-icon-copy-document"
                  @click="copy(`export ANTHROPIC_BASE_URL=${baseUrl}\nexport ANTHROPIC_AUTH_TOKEN=sk-你的密钥`)" />
              </div>
              <p>保存后执行使其生效：</p>
              <div class="code-block"><code>source ~/.zshrc</code></div>
            </el-tab-pane>

            <!-- Windows PowerShell -->
            <el-tab-pane label="Windows PowerShell" name="ps">
              <p>永久写入当前用户的环境变量（重启终端后生效）：</p>
              <div class="code-block">
                <code>[System.Environment]::SetEnvironmentVariable("ANTHROPIC_BASE_URL","{{ baseUrl }}","User")<br>[System.Environment]::SetEnvironmentVariable("ANTHROPIC_AUTH_TOKEN","sk-你的密钥","User")</code>
                <el-button type="text" class="copy-btn" icon="el-icon-copy-document"
                  @click="copy(`[System.Environment]::SetEnvironmentVariable(\"ANTHROPIC_BASE_URL\",\"${baseUrl}\",\"User\")\n[System.Environment]::SetEnvironmentVariable(\"ANTHROPIC_AUTH_TOKEN\",\"sk-你的密钥\",\"User\")`)" />
              </div>
            </el-tab-pane>

          </el-tabs>
        </div>
      </el-step>

      <!-- Step 5 -->
      <el-step title="启动 Claude Code">
        <div slot="description" class="step-body">
          <p>进入你的项目目录，运行：</p>
          <div class="code-block">
            <code>claude</code>
            <el-button type="text" class="copy-btn" icon="el-icon-copy-document"
              @click="copy('claude')" />
          </div>
          <p>首次运行会提示登录，选择 <strong>「跳过登录 / Skip login」</strong>（因为密钥已通过环境变量配置好了）。</p>
          <el-alert type="success" show-icon :closable="false">
            <span slot="title">成功后即可在终端中直接向 Claude 发起对话，开始 AI 辅助编程！</span>
          </el-alert>
        </div>
      </el-step>

    </el-steps>

    <!-- 常见问题 -->
    <el-card shadow="never" class="faq-card">
      <div slot="header"><strong>常见问题</strong></div>
      <el-collapse>
        <el-collapse-item title="Q：出现 401 Unauthorized 怎么办？" name="1">
          <p>请检查 <code class="inline">ANTHROPIC_API_KEY</code> 是否正确复制，Key 不包含多余的空格。也可在「我的密钥」页面确认该 Key 状态是否为「正常」。</p>
        </el-collapse-item>
        <el-collapse-item title="Q：提示 Token 额度耗尽怎么办？" name="2">
          <p>请前往「我的密钥」页面查看剩余额度。额度耗尽后需联系管理员为你的 Key 增加额度。</p>
        </el-collapse-item>
        <el-collapse-item title="Q：Key 即将过期怎么办？" name="3">
          <p>「我的密钥」页面会在截止日期不足 1 天时以红色高亮提示。请及时联系管理员续期，或吊销旧 Key 后重新申请。</p>
        </el-collapse-item>
        <el-collapse-item title="Q：claude 命令找不到（command not found）？" name="4">
          <p>npm 全局安装目录可能不在 PATH 中。可尝试：</p>
          <div class="code-block"><code>npm config get prefix</code></div>
          <p>将该路径下的 <code class="inline">bin</code> 目录添加到系统 PATH，然后重启终端。</p>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script>
import { copyText } from '@/utils/clipboard'

export default {
  name: 'PortalGuide',
  data() {
    return {
      cfgTab: 'json'
    }
  },
  computed: {
    baseUrl() {
      return window.location.origin + (process.env.VUE_APP_BASE_PATH || '')
    },
    settingsJson() {
      const base = this.baseUrl
      return `{
  "env": {
    "ANTHROPIC_AUTH_TOKEN": "sk-你的密钥",
    "ANTHROPIC_BASE_URL": "${base}",
    "CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC": 1
  },
  "permissions": {
    "allow": [],
    "deny": []
  }
}`
    }
  },
  methods: {
    copy(text) {
      copyText(text).then(() => {
        this.$message.success('已复制')
      })
    }
  }
}
</script>

<style scoped>
.guide-wrap { max-width: 820px; }
.page-title { font-size: 18px; font-weight: 600; color: #303133; margin-bottom: 24px; }

.steps { margin-bottom: 32px; }
.step-body { padding: 8px 0 16px; color: #606266; font-size: 14px; line-height: 1.8; }
.step-body p { margin: 6px 0; }

.code-block {
  position: relative;
  background: #1e1e2e;
  border-radius: 6px;
  padding: 12px 42px 12px 16px;
  margin: 8px 0;
  overflow-x: auto;
}
.code-block code {
  color: #cdd6f4;
  font-family: 'Consolas', 'Menlo', monospace;
  font-size: 13px;
  white-space: pre;
}
.copy-btn {
  position: absolute;
  top: 6px;
  right: 6px;
  color: #6c7086 !important;
  font-size: 16px !important;
  padding: 4px !important;
}
.copy-btn:hover { color: #cdd6f4 !important; }

.inline {
  background: #f0f2f5;
  border-radius: 3px;
  padding: 1px 5px;
  font-family: 'Consolas', 'Menlo', monospace;
  font-size: 13px;
  color: #e06c75;
}

.link { color: #409EFF; text-decoration: none; }
.link:hover { text-decoration: underline; }

.os-tabs { margin-top: 10px; }
.tip { color: #909399; font-size: 13px; }

.faq-card { margin-top: 8px; }
.faq-card p { margin: 6px 0; font-size: 14px; color: #606266; line-height: 1.7; }
</style>
