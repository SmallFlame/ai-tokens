<template>
  <div>
    <div class="page-title">我的 API 密钥</div>

    <el-card shadow="never">
      <div slot="header" style="display:flex;justify-content:space-between;align-items:center">
        <span>密钥列表</span>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="openCreate">
          新建密钥
        </el-button>
      </div>

      <el-table :data="list" stripe>
        <el-table-column label="名称" prop="name" min-width="120" />
        <el-table-column label="密钥" min-width="280">
          <template slot-scope="{ row }">
            <span class="key-text">{{ visibleKeys[row.id] ? row.keyValue : maskKey(row.keyValue) }}</span>
            <el-button type="text" :icon="visibleKeys[row.id] ? 'el-icon-view' : 'el-icon-hide'" @click="toggleVisible(row.id)" />
            <el-button type="text" icon="el-icon-copy-document" @click="copy(row.keyValue)" />
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template slot-scope="{ row }">
            <el-tag :type="row.keyType === 1 ? 'warning' : 'success'" size="mini">
              {{ row.keyType === 1 ? 'Token额度' : '金额消费' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="绑定渠道" width="160">
          <template slot-scope="{ row }">
            <div class="channel-cell">
              <el-tag v-if="row.channelId" type="info" size="mini" class="channel-tag">
                {{ channelName(row.channelId) }}
              </el-tag>
              <span v-else class="auto-text">自动</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="已用金额" width="120">
          <template slot-scope="{ row }">
            <span v-if="row.usedMoney && row.usedMoney > 0">¥ {{ row.usedMoney }}</span>
            <span v-else class="quota-text">未消费</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="mini">
              {{ row.status === 1 ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="额度/余额" min-width="200">
          <template slot-scope="{ row }">
            <span v-if="row.keyType !== 1" class="quota-text">消费时扣用户余额</span>
            <template v-else>
              <div v-if="row.totalQuota === -1" class="quota-text">
                已用 {{ (row.usedQuota || 0).toLocaleString() }} / 不限制
              </div>
              <div v-else>
                <el-progress
                  :percentage="Math.min(100, Math.round((row.usedQuota || 0) / row.totalQuota * 100))"
                  :status="(row.usedQuota || 0) >= row.totalQuota ? 'exception' : null"
                  :stroke-width="10"
                />
                <span class="quota-text">
                  已用 {{ (row.usedQuota || 0).toLocaleString() }} / {{ row.totalQuota.toLocaleString() }}
                  &nbsp;|&nbsp;
                  <span :class="(row.totalQuota - (row.usedQuota || 0)) <= 0 ? 'quota-exhausted' : ''">
                    剩余 {{ Math.max(0, row.totalQuota - (row.usedQuota || 0)).toLocaleString() }}
                  </span>
                </span>
              </div>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="截止时间" width="170">
          <template slot-scope="{ row }">
            <span v-if="!row.expiredAt" class="quota-text">永不过期</span>
            <span v-else :class="isExpiringSoon(row.expiredAt) ? 'expire-soon' : ''">
              {{ formatDate(row.expiredAt) }}
              <span v-if="isExpiringSoon(row.expiredAt)"> (即将到期)</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" @click="openEditChannel(row)">换渠道</el-button>
            <el-popconfirm title="确认吊销此密钥？" @confirm="revoke(row.id)">
              <el-button slot="reference" type="text" style="color:#F56C6C">吊销</el-button>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建密钥对话框 -->
    <el-dialog title="新建密钥" :visible.sync="showCreate" width="460px" @close="resetCreateForm">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="createForm.name" placeholder="如：我的开发用密钥" />
        </el-form-item>
        <el-form-item label="绑定渠道">
          <el-select v-model="createForm.channelId" placeholder="不选则自动负载均衡" clearable style="width:100%">
            <el-option
              v-for="c in channels"
              :key="c.channelId"
              :label="c.channelName"
              :value="c.channelId"
            />
          </el-select>
          <div class="form-tip">
            绑定后固定走该渠道计费；不绑定则系统自动选择。
            <router-link to="/portal/channels">查看渠道价格</router-link>
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doCreate">创建</el-button>
      </div>
    </el-dialog>

    <!-- 修改绑定渠道对话框 -->
    <el-dialog title="修改绑定渠道" :visible.sync="showEditChannel" width="420px">
      <div v-if="editingKey" class="editing-key-info">
        密钥：<span class="key-text">{{ editingKey.name }}</span>
      </div>
      <el-form label-width="80px" style="margin-top:16px">
        <el-form-item label="绑定渠道">
          <el-select v-model="editChannelId" placeholder="不选则自动负载均衡" clearable style="width:100%">
            <el-option
              v-for="c in channels"
              :key="c.channelId"
              :label="c.channelName"
              :value="c.channelId"
            />
          </el-select>
          <div class="form-tip">清空选择后，此密钥将使用系统负载均衡自动路由</div>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showEditChannel = false">取消</el-button>
        <el-button type="primary" :loading="savingChannel" @click="doUpdateChannel">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getKeyList, createKey, revokeKey, updateKeyChannel } from '@/api/apikey'
import { getUserChannels, getUserDefaultChannel } from '@/api/channel'
import { copyText } from '@/utils/clipboard'

export default {
  name: 'PortalMyKeys',
  data() {
    return {
      list: [],
      channels: [],
      defaultChannelId: null,
      visibleKeys: {},
      // 新建
      showCreate: false,
      saving: false,
      createForm: { name: '', channelId: null },
      // 修改渠道
      showEditChannel: false,
      savingChannel: false,
      editingKey: null,
      editChannelId: null
    }
  },
  created() {
    this.load()
    this.loadChannels()
  },
  methods: {
    async load() {
      this.list = await getKeyList()
    },
    async loadChannels() {
      try { this.channels = await getUserChannels() || [] } catch (e) {}
      try { this.defaultChannelId = await getUserDefaultChannel() || null } catch (e) {}
    },
    openCreate() {
      this.createForm = { name: '', channelId: this.defaultChannelId }
      this.showCreate = true
    },
    resetCreateForm() {
      this.createForm = { name: '', channelId: this.defaultChannelId }
    },
    async doCreate() {
      if (!this.createForm.name) return this.$message.warning('请输入密钥名称')
      this.saving = true
      try {
        await createKey({ name: this.createForm.name, channelId: this.createForm.channelId || null })
        this.$message.success('创建成功')
        this.showCreate = false
        this.load()
      } finally {
        this.saving = false
      }
    },
    openEditChannel(row) {
      this.editingKey = row
      this.editChannelId = row.channelId || null
      this.showEditChannel = true
    },
    async doUpdateChannel() {
      this.savingChannel = true
      try {
        await updateKeyChannel(this.editingKey.id, this.editChannelId || undefined)
        this.editingKey.channelId = this.editChannelId || null
        this.$message.success('已更新绑定渠道')
        this.showEditChannel = false
      } finally {
        this.savingChannel = false
      }
    },
    async revoke(id) {
      await revokeKey(id)
      this.$message.success('已吊销')
      this.load()
    },
    channelName(id) {
      const c = this.channels.find(c => c.channelId === id)
      return c ? c.channelName : `渠道${id}`
    },
    copy(text) {
      copyText(text).then(() => this.$message.success('已复制'))
    },
    maskKey(key) {
      if (!key) return '****'
      if (key.length <= 12) return key.slice(0, 4) + '****'
      return key.slice(0, 8) + '****' + key.slice(-4)
    },
    toggleVisible(id) {
      this.$set(this.visibleKeys, id, !this.visibleKeys[id])
    },
    formatDate(val) {
      if (!val) return ''
      return new Date(val).toLocaleString('zh-CN', { hour12: false })
    },
    isExpiringSoon(val) {
      if (!val) return false
      const diff = new Date(val).getTime() - Date.now()
      return diff > 0 && diff <= 24 * 60 * 60 * 1000
    }
  }
}
</script>

<style scoped>
.page-title { font-size: 18px; font-weight: 600; color: #303133; margin-bottom: 20px; }
.key-text {
  font-family: monospace;
  font-size: 13px;
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  margin-right: 4px;
}
.channel-cell { display: flex; align-items: center; gap: 4px; }
.channel-tag  { max-width: 110px; overflow: hidden; text-overflow: ellipsis; }
.auto-text    { font-size: 12px; color: #C0C4CC; }
.edit-btn     { padding: 0; font-size: 13px; color: #909399; }
.edit-btn:hover { color: #409EFF; }
.switch-btn   { padding: 2px 4px; font-size: 12px; color: #409EFF; }
.editing-key-info { font-size: 13px; color: #606266; padding: 0 4px; }
.quota-text   { font-size: 13px; color: #909399; }
.quota-exhausted { color: #F56C6C; }
.expire-soon  { color: #F56C6C; font-weight: 600; }
.form-tip     { font-size: 12px; color: #909399; margin-top: 4px; line-height: 1.5; }
</style>
