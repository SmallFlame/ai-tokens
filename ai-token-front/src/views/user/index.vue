<template>
  <div class="page-card">
    <!-- 筛选 -->
    <div class="toolbar">
      <el-input v-model="query.username" placeholder="用户名" clearable style="width:180px" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width:120px">
        <el-option :value="1" label="启用" />
        <el-option :value="0" label="禁用" />
      </el-select>
      <el-select v-model="query.groupStatus" placeholder="组状态" clearable style="width:120px">
        <el-option :value="0" label="组外" />
        <el-option :value="1" label="组内" />
      </el-select>
      <el-button type="primary" icon="el-icon-search" @click="search">查询</el-button>
      <el-button icon="el-icon-refresh" @click="reset">重置</el-button>
      <div style="margin-left:auto;display:flex;align-items:center;gap:8px">
        <el-button @click="selectAll" :loading="selectingAll">全选所有用户</el-button>
        <el-button
          type="warning"
          :disabled="selectedIds.size === 0"
          @click="openBatch"
        >批量分配 Tokens ▼ {{ selectedIds.size }} 人</el-button>
        <el-button
          type="primary"
          plain
          :disabled="selectedIds.size === 0"
          @click="openBatchMoney"
        >批量设置金额 {{ selectedIds.size }} 人</el-button>
        <el-button type="success" icon="el-icon-plus" @click="openCreate">新建账号</el-button>
      </div>
    </div>

    <el-table
      ref="tableRef"
      :data="list"
      border
      stripe
      v-loading="loading"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="id"        label="ID"    width="60" />
      <el-table-column prop="username"  label="用户名" min-width="120" />
      <el-table-column prop="nickname"  label="真实姓名" min-width="100" show-overflow-tooltip>
        <template slot-scope="{ row }">
          {{ row.nickname || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="email"     label="邮箱"   min-width="160" show-overflow-tooltip />
      <el-table-column label="角色" width="100">
        <template slot-scope="{ row }">
          <el-tag :type="roleTagType(row.roleId)" size="mini">
            {{ roleLabel(row.roleId) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="余额（元）" width="160">
        <template slot-scope="{ row }">
          <div v-if="row.moneyQuota == null" class="quota-text">未设置</div>
          <div v-else>
            <span :class="row.moneyQuota <= 0 ? 'quota-exhausted' : 'balance-ok'">
              ¥{{ fmtMoney(row.moneyQuota) }}
            </span>
            <span v-if="row.usedMoney" class="quota-text"> / 累计消费 ¥{{ fmtMoney(row.usedMoney) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template slot-scope="{ row }">
          <el-switch
            v-model="row.status"
            :active-value="1" :inactive-value="0"
            :disabled="roleCode(row.roleId) === 'super_admin'"
            @change="val => toggleStatus(row, val)"
          />
        </template>
      </el-table-column>
      <el-table-column label="组状态" width="80" align="center">
        <template slot-scope="{ row }">
          <el-tag :type="row.groupStatus === 1 ? 'success' : 'info'" size="mini">
            {{ row.groupStatus === 1 ? '组内' : '组外' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="180" />
      <el-table-column label="操作" min-width="200" fixed="right">
        <template slot-scope="{ row }">
          <el-button v-if="roleCode(row.roleId) !== 'super_admin'" type="text" @click="openMoney(row)">充值余额</el-button>
          <el-button v-if="roleCode(row.roleId) !== 'super_admin'" type="text" @click="resetPwd(row)">重置密码</el-button>
          <el-button v-if="roleCode(row.roleId) !== 'super_admin'" type="text" :disabled="!isSuperAdmin" @click="openRoleEdit(row)">改角色</el-button>
          <el-popconfirm v-if="roleCode(row.roleId) !== 'super_admin'" title="确认冻结此用户？冻结后可联系管理员解冻恢复" @confirm="del(row.id)">
            <el-button slot="reference" type="text" style="color:#F56C6C">冻结</el-button>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrap">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.pageNum"
        @current-change="p => { query.pageNum = p; loadList() }"
      />
    </div>

    <!-- 新建账号对话框 -->
    <el-dialog title="新建账号" :visible.sync="showCreate" width="440px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="form.nickname" placeholder="选填" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" show-password placeholder="初始密码（至少6位）" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="可选" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleId" style="width:100%">
            <el-option
              v-for="r in roleList"
              :key="r.id"
              :value="r.id"
              :label="r.name"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doCreate">创建</el-button>
      </div>
    </el-dialog>

    <!-- 充值余额对话框 -->
    <el-dialog title="充值/扣减余额" :visible.sync="showMoney" width="420px">
      <el-form label-width="110px">
        <el-form-item label="用户">
          <span class="form-username">{{ moneyForm.username }}</span>
          <span style="margin-left:12px;color:#909399;font-size:13px">
            当前余额：¥{{ fmtMoney(moneyForm.currentBalance) }}
          </span>
        </el-form-item>
        <el-form-item label="操作">
          <el-radio-group v-model="moneyForm.mode">
            <el-radio label="add">充值（增加）</el-radio>
            <el-radio label="sub">扣减（减少）</el-radio>
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
        <el-form-item label="操作后余额">
          <span v-if="moneyForm.mode === 'add'" style="color:#67C23A;font-weight:500">
            ¥{{ fmtMoney((moneyForm.currentBalance || 0) + (moneyForm.amount || 0)) }}
          </span>
          <span v-else style="color:#F56C6C;font-weight:500">
            ¥{{ fmtMoney(Math.max(0, (moneyForm.currentBalance || 0) - (moneyForm.amount || 0))) }}
          </span>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showMoney = false">取消</el-button>
        <el-button type="primary" :loading="moneySaving" @click="doSetMoney">确认</el-button>
      </div>
    </el-dialog>

    <!-- 修改角色对话框 -->
    <el-dialog title="修改用户角色" :visible.sync="showRole" width="380px">
      <el-form label-width="80px">
        <el-form-item label="用户名">
          <span class="form-username">{{ roleForm.username }}</span>
        </el-form-item>
        <el-form-item label="新角色">
          <el-select v-model="roleForm.roleId" style="width:100%">
            <el-option
              v-for="r in roleList"
              :key="r.id"
              :value="r.id"
              :label="r.name"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showRole = false">取消</el-button>
        <el-button type="primary" :loading="roleSaving" @click="doChangeRole">确认</el-button>
      </div>
    </el-dialog>

    <!-- 批量分配 Token 对话框 -->
    <el-dialog title="批量分配 Tokens" :visible.sync="showBatch" width="520px" @close="onBatchClose">
      <el-form :model="batchForm" ref="batchFormRef" label-width="100px">
        <el-form-item label="已选用户">
          <div style="display:flex;flex-wrap:wrap;gap:6px;max-height:120px;overflow-y:auto">
            <el-tag
              v-for="uid in [...selectedIds]"
              :key="uid"
              closable
              size="small"
              @close="removeSelectedId(uid)"
            >{{ getUsername(uid) || uid }}</el-tag>
          </div>
          <div v-if="selectedIds.size === 0" style="color:#999;font-size:12px">暂无选中用户</div>
        </el-form-item>

        <el-form-item label="分配方式">
          <el-radio-group v-model="batchForm.mode">
            <el-radio label="create">新建 Key</el-radio>
            <el-radio label="update">更新最新 Key</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="batchForm.mode === 'create'" label="Key 名称"
          prop="name" :rules="[{required:true, message:'请输入 Key 名称', trigger:'blur'}]">
          <el-input v-model="batchForm.name" placeholder="如：批量分配-20260303" />
        </el-form-item>

        <el-form-item label="金额（元）" prop="amountYuan"
          :rules="[{required:true, message:'请输入金额', trigger:'blur'},
                   {type:'number', min:0.01, message:'金额需大于0', trigger:'blur'}]">
          <el-input-number
            v-model="batchForm.amountYuan"
            :min="0.01"
            :precision="2"
            style="width:200px"
          />
        </el-form-item>

        <el-form-item label="换算比率">
          <el-input-number v-model="batchForm.rate" :min="1" :precision="0" style="width:150px" />
          <span style="margin-left:8px;color:#666">token / 元</span>
        </el-form-item>

        <el-form-item label="折算 Token">
          <span style="color:#999">{{ computedTokens | formatNum }} tokens</span>
        </el-form-item>

        <el-form-item label="过期时间">
          <el-date-picker
            v-model="batchForm.expiredAt"
            type="datetime"
            placeholder="不填则永不过期"
            value-format="yyyy-MM-dd HH:mm:ss"
            style="width:220px"
          />
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button @click="showBatch = false">取消</el-button>
        <el-button type="primary" :loading="batchSaving" @click="doBatchAssign">确认分配</el-button>
      </div>
    </el-dialog>

    <!-- 批量设置金额额度对话框 -->
    <el-dialog title="批量设置金额额度" :visible.sync="showBatchMoney" width="480px">
      <el-form label-width="110px">
        <el-form-item label="已选用户">
          <div style="display:flex;flex-wrap:wrap;gap:6px;max-height:100px;overflow-y:auto">
            <el-tag
              v-for="uid in [...selectedIds]"
              :key="uid"
              closable
              size="small"
              @close="removeSelectedId(uid)"
            >{{ getUsername(uid) || uid }}</el-tag>
          </div>
          <div v-if="selectedIds.size === 0" style="color:#999;font-size:12px">暂无选中用户</div>
        </el-form-item>
        <el-form-item label="金额额度（元）">
          <el-input-number
            v-model="batchMoneyForm.moneyQuota"
            :min="0"
            :precision="2"
            style="width:180px"
          />
          <div class="form-tip">例如：100 表示每人 100 元额度</div>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showBatchMoney = false">取消</el-button>
        <el-button type="primary" :loading="batchMoneySaving" @click="doBatchMoney">确认设置</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getUserList, updateUserStatus, rechargeUserMoney, updateUserRole, batchSetMoney, resetUserPassword } from '@/api/user'
import { createUser, deleteUser } from '@/api/my'
import { adminBatchAssign } from '@/api/apikey'
import { getRoleList } from '@/api/role'

export default {
  name: 'UserPage',
  filters: {
    formatNum(val) {
      if (!val && val !== 0) return '0'
      return Number(val).toLocaleString()
    }
  },
  data() {
    const today = new Date()
    const dateStr = `${today.getFullYear()}${String(today.getMonth()+1).padStart(2,'0')}${String(today.getDate()).padStart(2,'0')}`
    return {
      loading: false,
      list: [],
      total: 0,
      query: { pageNum: 1, pageSize: 20, username: '', status: null, groupStatus: null },
      showCreate: false,
      saving: false,
      roleList: [],
      roleMap: {},
      form: { username: '', nickname: '', password: '', email: '', roleId: null },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }]
      },
      // 批量选择
      selectedIds: new Set(),
      selectingAll: false,
      // 批量分配对话框
      showBatch: false,
      batchSaving: false,
      batchForm: {
        mode: 'create',
        name: `批量分配-${dateStr}`,
        amountYuan: null,
        rate: 1000,
        expiredAt: null
      },
      // 余额充值
      showMoney: false,
      moneySaving: false,
      moneyForm: { userId: null, username: '', currentBalance: 0, mode: 'add', amount: null },
      // 修改角色
      showRole: false,
      roleSaving: false,
      roleForm: { userId: null, username: '', roleId: null },
      // 批量设置金额
      showBatchMoney: false,
      batchMoneySaving: false,
      batchMoneyForm: { moneyQuota: 100 }
    }
  },
  computed: {
    ...mapGetters(['isSuperAdmin']),
    computedTokens() {
      if (!this.batchForm.amountYuan || !this.batchForm.rate) return 0
      return Math.round(this.batchForm.amountYuan * this.batchForm.rate)
    }
  },
  created() { this.init(); this.loadList() },
  methods: {
    async init() {
      try {
        this.roleList = await getRoleList()
        this.roleMap = {}
        this.roleList.forEach(r => {
          this.roleMap[r.id] = r
        })
        // 默认选组员
        const member = this.roleList.find(r => r.code === 'member')
        if (member) this.form.roleId = member.id
      } catch { /* ignore */ }
    },
    async loadList() {
      this.loading = true
      try {
        const page = await getUserList(this.query)
        this.list  = page.records
        this.total = page.total
        this.$nextTick(() => this.syncTableSelection())
      } finally { this.loading = false }
    },
    search() { this.query.pageNum = 1; this.loadList() },
    reset() { this.query = { pageNum: 1, pageSize: 20, username: '', status: null, groupStatus: null }; this.loadList() },
    async toggleStatus(row, val) {
      try {
        await updateUserStatus(row.id, val)
        this.$message.success(val === 1 ? '已启用' : '已禁用')
      } catch {
        row.status = val === 1 ? 0 : 1
      }
    },
    openCreate() {
      const member = this.roleList.find(r => r.code === 'member')
      this.form = { username: '', password: '', email: '', roleId: member ? member.id : null }
      this.showCreate = true
    },
    async doCreate() {
      try { await this.$refs.formRef.validate() } catch { return }
      this.saving = true
      try {
        await createUser(this.form)
        this.$message.success('账号创建成功')
        this.showCreate = false
        this.loadList()
      } finally {
        this.saving = false
      }
    },
    async del(id) {
      await deleteUser(id)
      this.$message.success('已冻结')
      this.loadList()
    },
    roleLabel(roleId) {
      const r = this.roleMap[roleId]
      return r ? r.name : (roleId || '-')
    },
    roleCode(roleId) {
      const r = this.roleMap[roleId]
      return r ? r.code : ''
    },
    roleTagType(roleId) {
      const code = this.roleCode(roleId)
      if (code === 'super_admin') return 'danger'
      if (code === 'leader') return 'warning'
      if (code === 'advisor') return 'success'
      return 'info'
    },
    // 余额充值
    openMoney(row) {
      this.moneyForm = {
        userId: row.id,
        username: row.username,
        currentBalance: row.moneyQuota == null ? 0 : Number(row.moneyQuota),
        mode: 'add',
        amount: null
      }
      this.showMoney = true
    },
    async doSetMoney() {
      if (!this.moneyForm.amount) return this.$message.warning('请输入金额')
      const delta = this.moneyForm.mode === 'add' ? this.moneyForm.amount : -this.moneyForm.amount
      this.moneySaving = true
      try {
        await rechargeUserMoney(this.moneyForm.userId, delta)
        this.$message.success('余额已' + (this.moneyForm.mode === 'add' ? '充值' : '扣减'))
        this.showMoney = false
        this.loadList()
      } finally { this.moneySaving = false }
    },
    // 重置密码
    async resetPwd(row) {
      try {
        await this.$confirm(`确认将用户 ${row.username} 的密码重置为 123456？`, '重置密码', {
          type: 'warning'
        })
      } catch { return }
      try {
        await resetUserPassword(row.id)
        this.$message.success('密码已重置为 123456')
      } catch { /* ignore */ }
    },
    // 修改角色
    openRoleEdit(row) {
      if (!this.isSuperAdmin) {
        this.$message.error('仅超级管理员可修改用户角色')
        return
      }
      this.roleForm = { userId: row.id, username: row.username, roleId: row.roleId }
      this.showRole = true
    },
    async doChangeRole() {
      this.roleSaving = true
      try {
        await updateUserRole(this.roleForm.userId, this.roleForm.roleId)
        this.$message.success('角色已修改')
        this.showRole = false
        this.loadList()
      } finally { this.roleSaving = false }
    },
    fmtMoney(val) {
      if (val == null) return '0.00'
      return Number(val).toFixed(2)
    },
    // 选择相关
    onSelectionChange(rows) {
      const pageIds = new Set(this.list.map(u => u.id))
      pageIds.forEach(id => this.selectedIds.delete(id))
      rows.forEach(row => this.selectedIds.add(row.id))
      this.selectedIds = new Set(this.selectedIds)
    },
    syncTableSelection() {
      if (!this.$refs.tableRef) return
      this.$refs.tableRef.clearSelection()
      this.list.forEach(row => {
        if (this.selectedIds.has(row.id)) {
          this.$refs.tableRef.toggleRowSelection(row, true)
        }
      })
    },
    async selectAll() {
      this.selectingAll = true
      try {
        const page = await getUserList({ pageNum: 1, pageSize: 9999 })
        const newSet = new Set(this.selectedIds)
        page.records.forEach(u => newSet.add(u.id))
        this.selectedIds = newSet
        this.$nextTick(() => this.syncTableSelection())
        this.$message.success(`已选中全部 ${newSet.size} 个用户`)
      } finally {
        this.selectingAll = false
      }
    },
    removeSelectedId(uid) {
      const newSet = new Set(this.selectedIds)
      newSet.delete(uid)
      this.selectedIds = newSet
      this.syncTableSelection()
    },
    getUsername(uid) {
      const user = this.list.find(u => u.id === uid)
      return user ? user.username : null
    },
    openBatch() {
      if (this.selectedIds.size === 0) {
        this.$message.warning('请先选择用户')
        return
      }
      this.showBatch = true
    },
    onBatchClose() {
      if (this.$refs.batchFormRef) this.$refs.batchFormRef.clearValidate()
    },
    async doBatchAssign() {
      try { await this.$refs.batchFormRef.validate() } catch { return }
      if (this.selectedIds.size === 0) {
        this.$message.warning('请先选择用户')
        return
      }
      const totalQuota = this.computedTokens
      this.batchSaving = true
      try {
        await adminBatchAssign({
          userIds: [...this.selectedIds],
          name: this.batchForm.name,
          totalQuota,
          expiredAt: this.batchForm.expiredAt || null,
          mode: this.batchForm.mode
        })
        this.$message.success(`已为 ${this.selectedIds.size} 个用户分配 ${totalQuota.toLocaleString()} tokens`)
        this.showBatch = false
        this.selectedIds = new Set()
        this.syncTableSelection()
      } finally {
        this.batchSaving = false
      }
    },
    openBatchMoney() {
      if (this.selectedIds.size === 0) {
        this.$message.warning('请先选择用户')
        return
      }
      this.showBatchMoney = true
    },
    async doBatchMoney() {
      if (this.batchMoneyForm.moneyQuota == null) {
        this.$message.warning('请输入金额额度')
        return
      }
      this.batchMoneySaving = true
      try {
        await batchSetMoney([...this.selectedIds], this.batchMoneyForm.moneyQuota)
        this.$message.success(`已为 ${this.selectedIds.size} 个用户设置金额额度 ¥${this.batchMoneyForm.moneyQuota}`)
        this.showBatchMoney = false
        this.loadList()
      } finally {
        this.batchMoneySaving = false
      }
    }
  }
}
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
.pagination-wrap { margin-top: 16px; text-align: right; }
.quota-text { font-size: 13px; color: #909399; }
.balance-ok { font-size: 13px; color: #67C23A; font-weight: 500; }
.quota-exhausted { font-size: 13px; color: #F56C6C; font-weight: 500; }
.form-tip   { font-size: 12px; color: #909399; margin-top: 4px; }
.form-username { font-weight: 600; color: #303133; }
</style>
