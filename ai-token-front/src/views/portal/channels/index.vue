<template>
  <div class="channels-wrap">
    <div class="page-title">渠道价格</div>
    <!-- <p class="page-desc">
      以下是当前可用的所有渠道及其模型定价。你可以在发送请求时通过
      <code class="inline">X-Channel-Id</code> 请求头指定渠道；
      不指定时系统会按照负载均衡自动选择。
    </p> -->

    <!-- 使用方法提示 -->
    <!-- <el-card class="usage-card" shadow="never">
      <div slot="header" class="card-header">
        <i class="el-icon-info" /> 如何指定渠道
      </div>
      <p class="tip-text">在请求头中加入 <code class="inline">X-Channel-Id: &lt;渠道ID&gt;</code>，例如：</p>
      <div class="code-block">
        <code>{{ curlExample }}</code>
        <el-button type="text" class="copy-btn" icon="el-icon-copy-document"
          @click="copy(curlExample)" />
      </div>
      <el-alert type="info" :closable="false" style="margin-top: 12px">
        <span slot="title">不添加此请求头时，系统依旧按原有优先级 + 加权随机负载均衡自动路由，无需任何改动。</span>
      </el-alert>
    </el-card> -->

    <!-- 渠道列表 -->
    <div v-loading="loading" class="channel-list">
      <el-empty v-if="!loading && channels.length === 0" description="暂无可用渠道" />

      <el-card
        v-for="ch in channels"
        :key="ch.channelId"
        class="channel-card"
        shadow="hover"
      >
        <div slot="header" class="channel-header">
          <span class="channel-name">
            <i class="el-icon-connection" />
            {{ ch.channelName }}
          </span>
          <el-tag type="info" size="small">ID: {{ ch.channelId }}</el-tag>
        </div>

        <el-table :data="ch.models" size="small" border stripe>
          <el-table-column label="模型别名（请求时使用）" min-width="180">
            <template slot-scope="{ row }">
              <code class="inline">{{ row.alias || row.modelName }}</code>
            </template>
          </el-table-column>
          <el-table-column label="真实模型名" min-width="160">
            <template slot-scope="{ row }">
              <span class="model-real">{{ row.modelName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="输入价格" min-width="130" align="right">
            <template slot-scope="{ row }">
              <div class="price">¥ {{ formatPrice(row.inputPrice) }}</div>
              <div class="price-note">/ 千 Token</div>
            </template>
          </el-table-column>
          <el-table-column label="输出价格" min-width="130" align="right">
            <template slot-scope="{ row }">
              <div class="price">¥ {{ formatPrice(row.outputPrice) }}</div>
              <div class="price-note">/ 千 Token</div>
            </template>
          </el-table-column>
          <el-table-column label="缓存创建" min-width="130" align="right">
            <template slot-scope="{ row }">
              <div class="price">¥ {{ formatPrice(row.cacheCreationPrice != null ? row.cacheCreationPrice : row.inputPrice) }}</div>
              <div class="price-note">/ 千 Token</div>
            </template>
          </el-table-column>
          <el-table-column label="缓存命中" min-width="130" align="right">
            <template slot-scope="{ row }">
              <div class="price cache-read">¥ {{ formatCacheRead(row) }}</div>
              <div class="price-note">/ 千 Token</div>
            </template>
          </el-table-column>
          <el-table-column label="换算参考" min-width="230" align="right">
            <template slot-scope="{ row }">
              <div class="conversion">
                <span class="conv-label">输入</span>
                <span class="conv-val">1元 ≈ {{ tokensPerYuan(row.inputPrice) }}k tokens</span>
              </div>
              <div class="conversion">
                <span class="conv-label">输出</span>
                <span class="conv-val">1元 ≈ {{ tokensPerYuan(row.outputPrice) }}k tokens</span>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="ch.models.length === 0" class="no-models">该渠道暂无可用模型</div>
      </el-card>
    </div>
  </div>
</template>

<script>
import { getUserChannels } from '@/api/channel'
import { copyText } from '@/utils/clipboard'

export default {
  name: 'ChannelsPage',
  data() {
    return {
      loading: false,
      channels: []
    }
  },
  computed: {
    curlExample() {
      const id = this.channels.length > 0 ? this.channels[0].channelId : 1
      return `curl https://your-domain/v1/chat/completions \\
  -H "Authorization: Bearer sk-你的密钥" \\
  -H "X-Channel-Id: ${id}" \\
  -H "Content-Type: application/json" \\
  -d '{"model":"claude-sonnet-4-6","messages":[{"role":"user","content":"Hello"}]}'`
    }
  },
  created() {
    this.fetchChannels()
  },
  methods: {
    async fetchChannels() {
      this.loading = true
      try {
        const res = await getUserChannels()
        this.channels = res || []
      } catch (e) {
        this.$message.error('获取渠道列表失败')
      } finally {
        this.loading = false
      }
    },
    formatPrice(val) {
      if (val == null) return '—'
      return Number(val).toFixed(4)
    },
    formatCacheRead(row) {
      if (row.cacheReadPrice != null) return Number(row.cacheReadPrice).toFixed(4)
      if (row.inputPrice == null) return '—'
      return (Number(row.inputPrice) * 0.1).toFixed(4)
    },
    tokensPerYuan(price) {
      if (!price || Number(price) === 0) return '—'
      const tokens = 1000 / Number(price)
      if (tokens >= 1000) return (tokens / 1000).toFixed(1)
      return tokens.toFixed(1)
    },
    copy(text) {
      copyText(text).then(() => this.$message.success('已复制到剪贴板'))
        .catch(() => this.$message.error('复制失败，请手动复制'))
    }
  }
}
</script>

<style scoped>
.channels-wrap {
  width: 95%;
    min-width: 1200px;
  margin: 0 auto;
}
.page-title {
  font-size: 26px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}
.page-desc {
  color: #606266;
  font-size: 18px;
  margin-bottom: 24px;
  line-height: 1.7;
}

.usage-card {
  margin-bottom: 28px;
  border-radius: 8px;
}
.card-header {
  font-size: 20px;
  font-weight: 500;
  color: #303133;
}
.tip-text {
  color: #606266;
  font-size: 18px;
  margin-bottom: 14px;
}

.code-block {
  position: relative;
  background: #282c34;
  border-radius: 6px;
  padding: 18px 52px 18px 20px;
  margin: 12px 0;
  overflow-x: auto;
}
.code-block code {
  color: #abb2bf;
  font-family: 'Consolas', 'Menlo', monospace;
  font-size: 18px;
  white-space: pre;
}
.copy-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  color: #6c7086 !important;
  font-size: 20px !important;
  padding: 6px !important;
}
.copy-btn:hover { color: #abb2bf !important; }

.channel-list {
  display: flex;
  font-size: 18px;
  flex-direction: column;
  gap: 24px;
}
.channel-card {
  border-radius: 8px;
}
.channel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.channel-name {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 10px;
}
.channel-name i {
  color: #409EFF;
  font-size: 22px;
}

.inline {
  background: #f0f2f5;
  border-radius: 4px;
  padding: 2px 8px;
  font-family: 'Consolas', 'Menlo', monospace;
  font-size: 18px;
  color: #e06c75;
}
.model-real {
  color: #909399;
  font-size: 18px;
}
.price {
  font-weight: 500;
  color: #303133;
  font-size: 18px;
}
.price-note {
  font-size: 16px;
  color: #909399;
}
.cache-read {
  color: #67C23A;
}
.conversion {
  display: flex;
  justify-content: space-between;
  font-size: 17px;
  line-height: 2;
}
.conv-label { color: #909399; }
.conv-val { color: #303133; font-weight: 500; }
.no-models {
  color: #909399;
  font-size: 18px;
  padding: 16px 0;
  text-align: center;
}

/* 覆盖element-ui表格样式，确保表格内字体也变大 */
::v-deep .el-table {
  font-size: 18px;
}
::v-deep .el-table th {
  font-size: 18px;
}
::v-deep .el-table td {
  font-size: 18px;
}
::v-deep .el-tag {
  font-size: 16px;
  height: 28px;
  line-height: 26px;
  padding: 0 10px;
}
::v-deep .el-alert__title {
  font-size: 18px;
}
::v-deep .el-empty__description {
  font-size: 18px;
}
</style>
