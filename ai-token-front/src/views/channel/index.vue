<template>
  <div class="page-card">
    <!-- 操作栏 -->
    <div class="toolbar">
      <el-button type="primary" icon="el-icon-plus" @click="openDialog()">新增渠道</el-button>
      <span v-if="defaultChannelId" style="margin-left:16px;font-size:13px;color:#909399">
        默认渠道：<el-tag type="warning" size="mini">{{ defaultChannelName }}</el-tag>
        <el-button type="text" size="mini" style="margin-left:6px" @click="clearDefault">清除</el-button>
      </span>
    </div>

    <!-- 表格 -->
    <el-table :data="list" border stripe v-loading="loading">
      <el-table-column prop="id"           label="ID"    width="60" />
      <el-table-column prop="name"         label="渠道名称" min-width="100">
        <template slot-scope="{ row }">
          {{ row.name }}
          <el-tag v-if="row.id === defaultChannelId" type="warning" size="mini" style="margin-left:4px">默认</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="type"         label="类型"  min-width="100">
        <template slot-scope="{ row }">
          <el-tag size="mini" type="info">{{ row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="baseUrl"      label="Base URL" min-width="180" show-overflow-tooltip />
      <el-table-column prop="weight"       label="权重"  min-width="60" />
      <el-table-column prop="priority"     label="优先级" min-width="70" />
      <el-table-column label="预算使用" min-width="180">
        <template slot-scope="{ row }">
          <div v-if="!row.totalMoney || row.totalMoney == 0" class="quota-text">未设预算</div>
          <div v-else>
            <el-progress
              :percentage="Math.min(100, Math.round((row.usedMoney || 0) / row.totalMoney * 100))"
              :status="(row.usedMoney || 0) >= row.totalMoney ? 'exception' : null"
              :stroke-width="8"
            />
            <span class="quota-text">¥{{ fmtMoney(row.usedMoney) }} / ¥{{ fmtMoney(row.totalMoney) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="健康状态" width="90">
        <template slot-scope="{ row }">
          <el-tag :type="row.healthStatus === 1 ? 'success' : 'danger'" size="mini">
            {{ row.healthStatus === 1 ? '正常' : '异常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="70">
        <template slot-scope="{ row }">
          <el-switch
            v-model="row.status"
            :active-value="1" :inactive-value="0"
            @change="val => toggleStatus(row, val)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="240" fixed="right">
        <template slot-scope="{ row }">
          <el-button type="text" @click="openDialog(row)">编辑</el-button>
          <el-button type="text" @click="openRecharge(row)">充值</el-button>
          <el-button type="text" @click="openLogs(row)">日志</el-button>
          <el-button type="text" @click="doHealthCheck(row)">检测</el-button>
          <el-button
            type="text"
            :style="row.id === defaultChannelId ? 'color:#E6A23C' : ''"
            @click="setDefault(row)"
          >{{ row.id === defaultChannelId ? '已为默认' : '设为默认' }}</el-button>
          <el-button type="text" class="danger" @click="doDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      :title="form.id ? '编辑渠道' : '新增渠道'"
      :visible.sync="dialogVisible"
      width="560px"
      @close="resetForm"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="渠道名称" prop="name">
          <el-input v-model="form.name" placeholder="如：OpenAI 主渠道" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" style="width:100%">
            <el-option value="openai"     label="OpenAI" />
            <el-option value="azure"      label="Azure OpenAI" />
            <el-option value="anthropic"  label="Anthropic (Claude)" />
            <el-option value="wenxin"     label="文心一言" />
            <el-option value="qianwen"    label="通义千问" />
            <el-option value="custom"     label="自定义" />
          </el-select>
        </el-form-item>
        <el-form-item label="Base URL" prop="baseUrl">
          <el-input v-model="form.baseUrl" placeholder="https://api.openai.com/v1" />
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="form.apiKey" show-password placeholder="sk-..." />
          <span v-if="form.id" class="form-tip">编辑时必须重新填写 API Key</span>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="权重">
              <el-input-number v-model="form.weight"   :min="1" :max="100" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级">
              <el-input-number v-model="form.priority" :min="0" :max="100" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="超时(ms)">
          <el-input-number v-model="form.timeoutMs" :min="1000" :step="1000" style="width:100%" />
          <span class="form-tip">Opus 等重型模型建议设为 300000（5 分钟）</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </div>
    </el-dialog>

    <!-- 充值弹窗 -->
    <el-dialog title="渠道充值/扣减预算" :visible.sync="rechargeVisible" width="440px">
      <el-form label-width="110px">
        <el-form-item label="渠道">
          <span style="font-weight:600">{{ rechargeForm.channelName }}</span>
          <span style="margin-left:12px;color:#909399;font-size:13px">
            当前预算：¥{{ fmtMoney(rechargeForm.totalMoney) }}｜已用：¥{{ fmtMoney(rechargeForm.usedMoney) }}
          </span>
        </el-form-item>
        <el-form-item label="操作">
          <el-radio-group v-model="rechargeForm.mode">
            <el-radio label="add">充值（增加预算）</el-radio>
            <el-radio label="sub">扣减（减少预算）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额（元）">
          <el-input-number v-model="rechargeForm.amount" :min="0.01" :precision="2" style="width:180px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="rechargeForm.remark" placeholder="可选" />
        </el-form-item>
        <el-form-item label="充值后预算">
          <span v-if="rechargeForm.mode === 'add'" style="color:#67C23A;font-weight:500">
            ¥{{ fmtMoney((rechargeForm.totalMoney || 0) + (rechargeForm.amount || 0)) }}
          </span>
          <span v-else style="color:#F56C6C;font-weight:500">
            ¥{{ fmtMoney(Math.max(0, (rechargeForm.totalMoney || 0) - (rechargeForm.amount || 0))) }}
          </span>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" :loading="rechargeSaving" @click="doRecharge">确认</el-button>
      </div>
    </el-dialog>

    <!-- 充值日志弹窗 -->
    <el-dialog :title="`充值日志 - ${logsChannelName}`" :visible.sync="logsVisible" width="600px">
      <el-table :data="logs" stripe size="small" v-loading="logsLoading">
        <el-table-column label="时间" prop="createdAt" width="160" />
        <el-table-column label="金额（元）" width="120">
          <template slot-scope="{ row }">
            <span :style="row.delta > 0 ? 'color:#67C23A' : 'color:#F56C6C'">
              {{ row.delta > 0 ? '+' : '' }}{{ fmtMoney(row.delta) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import {
  getChannelList, createChannel, updateChannel,
  updateChannelStatus, deleteChannel, healthCheck,
  getAdminDefaultChannel, setAdminDefaultChannel,
  rechargeChannel, getChannelRechargeLogs
} from '@/api/channel'

const defaultForm = () => ({
  id: null, name: '', type: 'openai', baseUrl: '', apiKey: '',
  weight: 1, priority: 0, timeoutMs: 300000, remark: ''
})

export default {
  name: 'ChannelPage',
  data() {
    return {
      loading: false,
      saving: false,
      list: [],
      defaultChannelId: null,
      dialogVisible: false,
      form: defaultForm(),
      rules: {
        name:    [{ required: true, message: '请输入渠道名称' }],
        type:    [{ required: true, message: '请选择类型' }],
        baseUrl: [{ required: true, message: '请输入 Base URL' }],
        apiKey:  [{ required: true, message: '请输入 API Key' }]
      },
      // 充值
      rechargeVisible: false,
      rechargeSaving: false,
      rechargeForm: { channelId: null, channelName: '', totalMoney: 0, usedMoney: 0, mode: 'add', amount: null, remark: '' },
      // 日志
      logsVisible: false,
      logsLoading: false,
      logsChannelName: '',
      logs: []
    }
  },
  computed: {
    defaultChannelName() {
      const c = this.list.find(c => c.id === this.defaultChannelId)
      return c ? c.name : `渠道${this.defaultChannelId}`
    }
  },
  created() {
    this.loadList()
    this.loadDefault()
  },
  methods: {
    async loadList() {
      this.loading = true
      try { this.list = await getChannelList() }
      finally { this.loading = false }
    },
    async loadDefault() {
      try { this.defaultChannelId = await getAdminDefaultChannel() || null } catch (e) {}
    },
    async setDefault(row) {
      if (row.id === this.defaultChannelId) return
      await setAdminDefaultChannel(row.id)
      this.defaultChannelId = row.id
      this.$message.success(`已将「${row.name}」设为默认渠道`)
    },
    async clearDefault() {
      await setAdminDefaultChannel(null)
      this.defaultChannelId = null
      this.$message.success('已清除默认渠道')
    },
    openDialog(row) {
      if (row) {
        this.form = {
          ...defaultForm(),
          id: row.id, name: row.name, type: row.type, baseUrl: row.baseUrl,
          apiKey: '',
          weight: row.weight, priority: row.priority,
          timeoutMs: row.timeoutMs, remark: row.remark
        }
      } else {
        this.form = defaultForm()
      }
      this.dialogVisible = true
    },
    resetForm() {
      this.$refs.formRef && this.$refs.formRef.resetFields()
    },
    async submit() {
      try { await this.$refs.formRef.validate() } catch { return }
      this.saving = true
      try {
        const payload = { ...this.form }
        if (this.form.id) {
          await updateChannel(this.form.id, payload)
          this.$message.success('更新成功')
        } else {
          await createChannel(payload)
          this.$message.success('创建成功')
        }
        this.dialogVisible = false
        this.loadList()
      } finally {
        this.saving = false
      }
    },
    async toggleStatus(row, val) {
      try {
        await updateChannelStatus(row.id, val)
      } catch {
        row.status = val === 1 ? 0 : 1
      }
    },
    async doHealthCheck(row) {
      const loading = this.$loading({ text: '检测中...' })
      try {
        const healthy = await healthCheck(row.id)
        row.healthStatus = healthy ? 1 : 0
        this.$message({ type: healthy ? 'success' : 'warning', message: healthy ? '渠道正常' : '渠道异常' })
      } finally {
        loading.close()
      }
    },
    doDelete(row) {
      this.$confirm(`确认删除渠道「${row.name}」？`, '警告', { type: 'warning' }).then(async () => {
        await deleteChannel(row.id)
        this.$message.success('删除成功')
        this.loadList()
      }).catch(() => {})
    },
    openRecharge(row) {
      this.rechargeForm = {
        channelId: row.id,
        channelName: row.name,
        totalMoney: row.totalMoney || 0,
        usedMoney: row.usedMoney || 0,
        mode: 'add',
        amount: null,
        remark: ''
      }
      this.rechargeVisible = true
    },
    async doRecharge() {
      if (!this.rechargeForm.amount) return this.$message.warning('请输入金额')
      const delta = this.rechargeForm.mode === 'add' ? this.rechargeForm.amount : -this.rechargeForm.amount
      this.rechargeSaving = true
      try {
        await rechargeChannel(this.rechargeForm.channelId, delta, this.rechargeForm.remark)
        this.$message.success('操作成功')
        this.rechargeVisible = false
        this.loadList()
      } finally { this.rechargeSaving = false }
    },
    async openLogs(row) {
      this.logsChannelName = row.name
      this.logsVisible = true
      this.logsLoading = true
      try {
        this.logs = await getChannelRechargeLogs(row.id)
      } finally { this.logsLoading = false }
    },
    fmtMoney(val) {
      if (val == null) return '0.00'
      return Number(val).toFixed(2)
    }
  }
}
</script>

<style scoped>
.form-tip { font-size: 12px; color: #E6A23C; margin-left: 4px; }
.danger { color: #F56C6C; }
.quota-text { font-size: 13px; color: #909399; }
</style>
