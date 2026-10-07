<template>
  <div>
    <!-- 顶部信息栏 -->
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px;flex-wrap:wrap;gap:10px">
      <div class="page-title">我的团队</div>
      <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap">
        <!-- <span class="quota-badge">
          Token 可分配：<strong>{{ allocatable.toLocaleString() }}</strong> tokens
        </span> -->
        <span class="money-badge">
          金额可分配：<strong>¥{{ fmtMoney(allocatableMoney) }}</strong>
        </span>
        <el-button type="warning" size="small" icon="el-icon-user-solid" @click="openPull">
          拉人入组
        </el-button>
        <!-- <el-button type="primary" size="small" icon="el-icon-plus" @click="openCreate">
          新建组员
        </el-button> -->
      </div>
    </div>

    <el-card shadow="never">
      <el-table :data="members" stripe v-loading="loading">
        <el-table-column prop="id"       label="ID"   width="60" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="真实姓名" min-width="100" show-overflow-tooltip>
          <template slot-scope="{ row }">
            {{ row.nickname || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="email"    label="邮箱"  min-width="150" show-overflow-tooltip />
        <el-table-column label="余额（元）" width="130">
          <template slot-scope="{ row }">
            <span :style="balanceStyle(row.moneyQuota)">
              ¥{{ fmtMoney(row.moneyQuota) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template slot-scope="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1" :inactive-value="0"
              @change="val => toggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="加入时间" width="160" />
        <el-table-column label="操作" width="210" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" style="color:#409EFF" @click="openRecharge(row)">充值</el-button>
            <el-button type="text" @click="openAdjustMoney(row)">调整余额</el-button>
            <el-button type="text" style="color:#E6A23C" @click="doRelease(row)">移出组</el-button>
            <el-popconfirm title="确认冻结该组员？冻结后可联系管理员解冻恢复" @confirm="del(row.id)">
              <el-button slot="reference" type="text" style="color:#F56C6C">冻结</el-button>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建组员对话框 -->
    <el-dialog title="新建组员" :visible.sync="showCreate" width="420px" @close="resetCreate">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" show-password placeholder="初始密码（至少6位）" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="可选" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doCreate">创建</el-button>
      </div>
    </el-dialog>

    <!-- 拉人入组对话框 -->
    <el-dialog title="拉人入组" :visible.sync="showPull" width="500px">
      <div style="margin-bottom:12px;color:#606266;font-size:13px">
        以下为尚未加入任何小组的组员，可将其拉入你的小组：
      </div>
      <el-input
        v-model="pullSearch"
        placeholder="搜索用户名"
        clearable
        prefix-icon="el-icon-search"
        style="margin-bottom:12px"
      />
      <el-table :data="filteredAvailableUsers" stripe size="small" v-loading="pullLoading" max-height="360">
        <el-table-column prop="id"       label="ID"   width="70" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="email"    label="邮箱"  min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="80" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" @click="doPull(row.id)">拉入</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 充值对话框 -->
    <el-dialog title="给组员充值" :visible.sync="showRecharge" width="380px" @close="resetRecharge">
      <el-form label-width="100px">
        <el-form-item label="组员">
          <span style="font-weight:600;color:#303133">{{ rechargeForm.username }}</span>
        </el-form-item>
        <el-form-item label="当前余额">
          <span :style="balanceStyle(rechargeForm.currentBalance)">
            ¥{{ fmtMoney(rechargeForm.currentBalance) }}
          </span>
        </el-form-item>
        <el-form-item label="我的金额池">
          <span style="color:#67C23A;font-weight:600">¥{{ fmtMoney(allocatableMoney) }}</span>
        </el-form-item>
        <el-form-item label="充值金额（元）">
          <el-input-number
            v-model="rechargeForm.amount"
            :min="0.01"
            :precision="2"
            style="width:160px"
          />
        </el-form-item>
        <el-form-item label="充值后余额">
          <span style="color:#409EFF;font-weight:600">
            ¥{{ fmtMoney((Number(rechargeForm.currentBalance) || 0) + (Number(rechargeForm.amount) || 0)) }}
          </span>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showRecharge = false">取消</el-button>
        <el-button type="primary" :loading="rechargeSaving" @click="doRecharge">确认充值</el-button>
      </div>
    </el-dialog>

    <!-- 调整余额对话框 -->
    <el-dialog title="调整组员余额" :visible.sync="showMoney" width="420px" @close="resetMoney">
      <el-form label-width="110px">
        <el-form-item label="组员">
          <span style="font-weight:600;color:#303133">{{ moneyForm.username }}</span>
        </el-form-item>
        <el-form-item label="当前余额">
          <span :style="balanceStyle(moneyForm.currentBalance)">
            ¥{{ fmtMoney(moneyForm.currentBalance) }}
          </span>
        </el-form-item>
        <el-form-item label="我的金额池">
          <span style="color:#67C23A;font-weight:600">¥{{ fmtMoney(allocatableMoney) }}</span>
        </el-form-item>
        <el-form-item label="操作">
          <el-radio-group v-model="moneyForm.mode">
            <el-radio label="add">新增</el-radio>
            <el-radio label="sub">减少</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额（元）">
          <el-input-number
            v-model="moneyForm.amount"
            :min="0.01"
            :precision="2"
            style="width:180px"
          />
        </el-form-item>
        <el-form-item label="组员调整后">
          <span :style="balanceStyle(moneyPreviewMember)">
            ¥{{ fmtMoney(moneyPreviewMember) }}
          </span>
        </el-form-item>
        <el-form-item label="池调整后">
          <span :style="balanceStyle(moneyPreviewPool)">
            ¥{{ fmtMoney(moneyPreviewPool) }}
          </span>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showMoney = false">取消</el-button>
        <el-button type="primary" :loading="moneySaving" @click="doAdjustMoney">确认</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getTeamMembers, createTeamMember, updateMemberStatus, deleteTeamMember,
  getMyQuota, getAvailableUsers, pullMember, releaseMember, adjustMemberMoney
} from '@/api/team'

export default {
  name: 'TeamPage',
  data() {
    return {
      loading: false,
      members: [],
      allocatable: 0,
      allocatableMoney: 0,
      // 新建组员
      showCreate: false,
      saving: false,
      form: { username: '', password: '', email: '' },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }]
      },
      // 拉人
      showPull: false,
      pullLoading: false,
      availableUsers: [],
      pullSearch: '',
      // 调整余额
      showMoney: false,
      moneySaving: false,
      moneyForm: { memberId: null, username: '', currentBalance: 0, mode: 'add', amount: null },
      // 充值
      showRecharge: false,
      rechargeSaving: false,
      rechargeForm: { memberId: null, username: '', currentBalance: 0, amount: null }
    }
  },
  computed: {
    filteredAvailableUsers() {
      if (!this.pullSearch) return this.availableUsers
      const search = this.pullSearch.toLowerCase()
      return this.availableUsers.filter(u => u.username.toLowerCase().includes(search))
    },
    moneyPreviewMember() {
      const base = Number(this.moneyForm.currentBalance) || 0
      const amt  = Number(this.moneyForm.amount) || 0
      return this.moneyForm.mode === 'add' ? base + amt : Math.max(0, base - amt)
    },
    moneyPreviewPool() {
      const pool = Number(this.allocatableMoney) || 0
      const amt  = Number(this.moneyForm.amount) || 0
      return this.moneyForm.mode === 'add' ? Math.max(0, pool - amt) : pool + amt
    }
  },
  created() {
    this.load()
    this.loadQuota()
  },
  methods: {
    async load() {
      this.loading = true
      try { this.members = await getTeamMembers() }
      finally { this.loading = false }
    },
    async loadQuota() {
      const data = await getMyQuota()
      this.allocatable      = data.allocatableQuota || 0
      this.allocatableMoney = data.allocatableMoney || 0
    },
    // 新建组员
    openCreate() { this.showCreate = true },
    resetCreate() {
      this.form = { username: '', password: '', email: '' }
      this.$refs.formRef && this.$refs.formRef.resetFields()
    },
    async doCreate() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        this.saving = true
        try {
          await createTeamMember(this.form)
          this.$message.success('组员创建成功')
          this.showCreate = false
          this.load()
        } finally { this.saving = false }
      })
    },
    async toggleStatus(row, val) {
      try {
        await updateMemberStatus(row.id, val)
        this.$message.success(val === 1 ? '已启用' : '已禁用')
      } catch {
        row.status = val === 1 ? 0 : 1
      }
    },
    async del(id) {
      await deleteTeamMember(id)
      this.$message.success('已冻结')
      this.load()
    },
    // 拉人入组
    async openPull() {
      this.showPull = true
      this.pullLoading = true
      try { this.availableUsers = await getAvailableUsers() }
      finally { this.pullLoading = false }
    },
    async doPull(userId) {
      await pullMember(userId)
      this.$message.success('已拉入小组')
      this.availableUsers = this.availableUsers.filter(u => u.id !== userId)
      this.load()
    },
    // 移出组
    async doRelease(row) {
      try {
        await this.$confirm(`确认将 "${row.username}" 移出小组？账号不会被删除，剩余余额将归还到你的金额池。`, '移出组', {
          confirmButtonText: '确认移出',
          cancelButtonText: '取消',
          type: 'warning'
        })
      } catch { return }
      await releaseMember(row.id)
      this.$message.success('已移出小组')
      this.load()
      this.loadQuota()
    },
    // 充值
    openRecharge(row) {
      this.rechargeForm = { memberId: row.id, username: row.username, currentBalance: Number(row.moneyQuota) || 0, amount: null }
      this.showRecharge = true
    },
    resetRecharge() {
      this.rechargeForm = { memberId: null, username: '', currentBalance: 0, amount: null }
    },
    async doRecharge() {
      if (!this.rechargeForm.amount || this.rechargeForm.amount <= 0) {
        return this.$message.warning('请输入充值金额')
      }
      if (this.rechargeForm.amount > Number(this.allocatableMoney)) {
        return this.$message.warning('充值金额超过可用金额池余额')
      }
      this.rechargeSaving = true
      try {
        await adjustMemberMoney(this.rechargeForm.memberId, this.rechargeForm.amount)
        this.$message.success('充值成功')
        this.showRecharge = false
        this.load()
        this.loadQuota()
      } finally { this.rechargeSaving = false }
    },
    // 调整余额
    openAdjustMoney(row) {
      const balance = row.moneyQuota == null ? 0 : Number(row.moneyQuota)
      this.moneyForm = { memberId: row.id, username: row.username, currentBalance: balance, mode: 'add', amount: null }
      this.showMoney = true
    },
    resetMoney() {
      this.moneyForm = { memberId: null, username: '', currentBalance: 0, mode: 'add', amount: null }
    },
    async doAdjustMoney() {
      if (!this.moneyForm.amount || this.moneyForm.amount <= 0) {
        return this.$message.warning('请输入大于 0 的金额')
      }
      const delta = this.moneyForm.mode === 'add' ? this.moneyForm.amount : -this.moneyForm.amount
      this.moneySaving = true
      try {
        await adjustMemberMoney(this.moneyForm.memberId, delta)
        this.$message.success(this.moneyForm.mode === 'add' ? '已新增余额' : '已减少余额')
        this.showMoney = false
        this.load()
        this.loadQuota()
      } finally { this.moneySaving = false }
    },
    balanceStyle(val) {
      const v = Number(val) || 0
      if (v <= 0)   return 'color:#F56C6C;font-weight:600'
      if (v <= 1)   return 'color:#E6A23C;font-weight:600'
      return 'color:#67C23A;font-weight:600'
    },
    fmtMoney(val) {
      if (val == null) return '0.00'
      return Number(val).toFixed(2)
    }
  }
}
</script>

<style scoped>
.page-title { font-size: 18px; font-weight: 600; color: #303133; }
.quota-badge {
  background: #ecf5ff;
  color: #409EFF;
  border: 1px solid #b3d8ff;
  border-radius: 6px;
  padding: 4px 12px;
  font-size: 13px;
}
.quota-badge strong { font-size: 15px; }
.money-badge {
  background: #f0f9eb;
  color: #67C23A;
  border: 1px solid #b3e19d;
  border-radius: 6px;
  padding: 4px 12px;
  font-size: 13px;
}
.money-badge strong { font-size: 15px; }
</style>
