<template>
  <div class="page-card">
    <!-- 筛选 + 操作栏 -->
    <div class="toolbar">
      <el-select v-model="filterUserId" placeholder="全部用户" clearable filterable style="width:180px" @change="loadList">
        <el-option v-for="u in users" :key="u.id" :label="u.username" :value="u.id" />
      </el-select>
      <el-button type="primary" icon="el-icon-plus" @click="openCreate">为用户创建 Key</el-button>
    </div>

    <el-table :data="list" border stripe v-loading="loading">
      <el-table-column label="用户" prop="username" width="100" />
      <el-table-column label="名称" prop="name" min-width="110" />
      <el-table-column label="Key 值" min-width="220">
        <template slot-scope="{ row }">
          <span class="key-text">{{ maskKey(row.keyValue) }}</span>
          <el-button type="text" icon="el-icon-copy-document" @click="copyKey(row.keyValue)" />
          <el-button type="text" icon="el-icon-view" @click="viewKey(row.keyValue)" />
        </template>
      </el-table-column>
      <el-table-column label="类型" width="90">
        <template slot-scope="{ row }">
          <el-tag :type="row.keyType === 1 ? 'warning' : 'success'" size="mini">
            {{ row.keyType === 1 ? 'Token额度' : '金额消费' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="绑定渠道" width="120">
        <template slot-scope="{ row }">
          <el-tag v-if="row.channelId" type="info" size="mini" class="channel-tag">
            {{ channelName(row.channelId) }}
          </el-tag>
          <span v-else class="auto-text">自动</span>
        </template>
      </el-table-column>
      <el-table-column label="已用金额" width="120">
        <template slot-scope="{ row }">
          <span v-if="row.usedMoney && row.usedMoney > 0">¥ {{ row.usedMoney }}</span>
          <span v-else class="quota-text">未消费</span>
        </template>
      </el-table-column>
      <el-table-column label="额度消耗" min-width="180">
        <template slot-scope="{ row }">
          <span v-if="row.keyType !== 1" class="quota-text">金额消费型（见用户余额）</span>
          <template v-else>
            <div v-if="row.totalQuota === -1" class="quota-text">
              已用 {{ row.usedQuota.toLocaleString() }} / 不限制
            </div>
            <div v-else>
              <el-progress
                :percentage="Math.min(100, Math.round(row.usedQuota / row.totalQuota * 100))"
                :status="row.usedQuota >= row.totalQuota ? 'exception' : null"
                :stroke-width="10"
              />
              <span class="quota-text">{{ row.usedQuota.toLocaleString() }} / {{ row.totalQuota.toLocaleString() }}</span>
            </div>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="过期时间" width="160">
        <template slot-scope="{ row }">{{ row.expiredAt ? formatDate(row.expiredAt) : '永不过期' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template slot-scope="{ row }">
          <el-switch
            v-model="row.status"
            :active-value="1" :inactive-value="0"
            @change="val => toggleStatus(row, val)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="220" fixed="right">
        <template slot-scope="{ row }">
          <el-button type="text" @click="openEdit(row)">编辑</el-button>
          <el-button type="text" @click="openEditChannel(row)">换渠道</el-button>
          <el-button type="text" class="danger" @click="doRevoke(row)">吊销</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 创建 Key 弹窗 -->
    <el-dialog title="为用户创建 API Key" :visible.sync="createVisible" width="460px" @close="resetCreate">
      <el-form :model="createForm" ref="createRef" label-width="90px">
        <el-form-item label="选择用户" prop="userId" :rules="[{ required: true, message: '请选择用户' }]">
          <el-select v-model="createForm.userId" filterable style="width:100%" placeholder="请选择">
            <el-option v-for="u in users" :key="u.id" :label="u.username" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Key 名称">
          <el-input v-model="createForm.name" placeholder="如：claude-code 专用" />
        </el-form-item>
        <el-form-item label="Key 类型">
          <el-radio-group v-model="createForm.keyType">
            <el-radio :label="2">金额消费型（扣用户余额）</el-radio>
            <el-radio :label="1">Token额度型（管理员分配Token）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="绑定渠道">
          <el-select v-model="createForm.channelId" placeholder="不选则自动负载均衡" clearable filterable style="width:100%">
            <el-option v-for="c in channels" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="createForm.keyType === 1" label="Token 额度">
          <el-input-number v-model="createForm.totalQuota" :min="-1" style="width:100%" />
          <div class="form-tip">-1 表示不限制</div>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="createForm.expiredAt"
            type="datetime"
            placeholder="不填写则永不过期"
            value-format="yyyy-MM-dd HH:mm:ss"
            style="width:100%"
          />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doCreate">创建</el-button>
      </div>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog title="编辑 API Key" :visible.sync="editVisible" width="420px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="Key 类型">
          <el-radio-group v-model="editForm.keyType">
            <el-radio :label="2">金额消费型</el-radio>
            <el-radio :label="1">Token额度型</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="editForm.keyType === 1" label="Token 额度">
          <el-input-number v-model="editForm.totalQuota" :min="-1" style="width:100%" />
          <div class="form-tip">-1 表示不限制</div>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="editForm.expiredAt"
            type="datetime"
            placeholder="不填写则永不过期"
            value-format="yyyy-MM-dd HH:mm:ss"
            style="width:100%"
          />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doEdit">保存</el-button>
      </div>
    </el-dialog>

    <!-- 修改绑定渠道弹窗 -->
    <el-dialog title="修改绑定渠道" :visible.sync="channelVisible" width="400px">
      <div v-if="editingChannelRow" class="editing-key-info">
        密钥：{{ editingChannelRow.name || editingChannelRow.keyValue }}
      </div>
      <el-form label-width="90px" style="margin-top:16px">
        <el-form-item label="绑定渠道">
          <el-select v-model="editChannelId" placeholder="不选则自动负载均衡" clearable filterable style="width:100%">
            <el-option v-for="c in channels" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <div class="form-tip">清空选择后，此密钥将使用系统负载均衡自动路由</div>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="channelVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingChannel" @click="doUpdateChannel">保存</el-button>
      </div>
    </el-dialog>

    <!-- 查看完整 Key 弹窗 -->
    <el-dialog title="API Key" :visible.sync="viewVisible" width="480px">
      <div class="key-box">
        <code>{{ currentKey }}</code>
        <el-button size="small" icon="el-icon-copy-document" @click="copyKey(currentKey)">复制</el-button>
      </div>
      <p class="key-warning"><i class="el-icon-warning" /> 请妥善保管，此 Key 等同于密码</p>
    </el-dialog>
  </div>
</template>

<script>
import {
  adminGetAllKeys, adminCreateKey, adminUpdateKey,
  adminUpdateKeyStatus, adminUpdateKeyChannel, adminRevokeKey
} from '@/api/apikey'
import { getChannelList } from '@/api/channel'
import { getUserList } from '@/api/my'
import { copyText } from '@/utils/clipboard'

export default {
  name: 'ApiKeyPage',
  data() {
    return {
      loading: false, saving: false,
      list: [], users: [], channels: [],
      filterUserId: null,
      createVisible: false,
      editVisible: false,
      viewVisible: false,
      channelVisible: false,
      savingChannel: false,
      currentKey: '',
      editingId: null,
      editingChannelRow: null,
      editChannelId: null,
      createForm: { userId: null, name: '', channelId: null, keyType: 2, totalQuota: -1, expiredAt: null },
      editForm:   { keyType: 2, totalQuota: -1, expiredAt: null }
    }
  },
  async created() {
    try {
      const page = await getUserList({ pageNum: 1, pageSize: 200 })
      this.users = page.records || []
    } catch (e) {}
    try { this.channels = await getChannelList() || [] } catch (e) {}
    this.loadList()
  },
  methods: {
    async loadList() {
      this.loading = true
      try { this.list = await adminGetAllKeys(this.filterUserId) }
      finally { this.loading = false }
    },
    openCreate() {
      this.createForm = { userId: null, name: '', channelId: null, keyType: 2, totalQuota: -1, expiredAt: null }
      this.createVisible = true
    },
    resetCreate() { this.$refs.createRef && this.$refs.createRef.resetFields() },
    async doCreate() {
      try { await this.$refs.createRef.validate() } catch { return }
      this.saving = true
      try {
        await adminCreateKey(this.createForm.userId, this.createForm)
        this.$message.success('创建成功')
        this.createVisible = false
        this.loadList()
      } finally { this.saving = false }
    },
    openEdit(row) {
      this.editingId = row.id
      this.editForm = { keyType: row.keyType || 2, totalQuota: row.totalQuota, expiredAt: row.expiredAt }
      this.editVisible = true
    },
    async doEdit() {
      this.saving = true
      try {
        await adminUpdateKey(this.editingId, this.editForm)
        this.$message.success('已更新')
        this.editVisible = false
        this.loadList()
      } finally { this.saving = false }
    },
    openEditChannel(row) {
      this.editingChannelRow = row
      this.editChannelId = row.channelId || null
      this.channelVisible = true
    },
    async doUpdateChannel() {
      this.savingChannel = true
      try {
        await adminUpdateKeyChannel(this.editingChannelRow.id, this.editChannelId || undefined)
        this.editingChannelRow.channelId = this.editChannelId || null
        this.$message.success('已更新绑定渠道')
        this.channelVisible = false
      } finally { this.savingChannel = false }
    },
    channelName(id) {
      const c = this.channels.find(c => c.id === id)
      return c ? c.name : `渠道${id}`
    },
    async toggleStatus(row, val) {
      try {
        await adminUpdateKeyStatus(row.id, val)
      } catch {
        row.status = val === 1 ? 0 : 1
      }
    },
    doRevoke(row) {
      this.$confirm(`确认吊销「${row.name || row.keyValue}」？吊销后不可恢复。`, '警告', { type: 'warning' })
        .then(async () => {
          await adminRevokeKey(row.id)
          this.$message.success('已吊销')
          this.loadList()
        }).catch(() => {})
    },
    viewKey(key) { this.currentKey = key; this.viewVisible = true },
    maskKey(key) {
      if (!key) return ''
      return key.slice(0, 8) + '****' + key.slice(-4)
    },
    async copyKey(key) {
      try {
        await copyText(key)
        this.$message.success('已复制到剪贴板')
      } catch {
        this.$message.warning('请手动复制')
      }
    },
    formatDate(val) {
      if (!val) return ''
      return new Date(val).toLocaleString('zh-CN', { hour12: false })
    }
  }
}
</script>

<style scoped>
.key-text   { font-family: monospace; color: #303133; }
.quota-text { font-size: 13px; color: #909399; }
.form-tip   { font-size: 12px; color: #909399; margin-top: 4px; }
.key-box    { display: flex; align-items: center; gap: 12px; background: #f5f7fa; padding: 14px; border-radius: 4px; }
.key-box code { flex: 1; word-break: break-all; font-size: 14px; }
.key-warning { margin-top: 14px; font-size: 14px; color: #E6A23C; }
.channel-tag  { max-width: 100px; overflow: hidden; text-overflow: ellipsis; }
.auto-text  { font-size: 12px; color: #C0C4CC; }
.editing-key-info { font-size: 13px; color: #606266; padding: 0 4px 8px; }
.danger { color: #F56C6C; }
</style>
