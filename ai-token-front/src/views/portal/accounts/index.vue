<template>
  <div>
    <div class="page-title">账号管理</div>

    <el-card shadow="never">
      <div slot="header" style="display:flex;justify-content:space-between;align-items:center">
        <span>用户列表</span>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="openCreate">新建账号</el-button>
      </div>

      <el-form inline size="small" style="margin-bottom:12px">
        <el-form-item label="用户名">
          <el-input v-model="search" placeholder="搜索用户名" clearable style="width:180px" @keyup.enter.native="load" />
        </el-form-item>
        <el-form-item>
          <el-button-group>
            <el-button :type="groupStatus === null ? 'primary' : ''" size="small" @click="setFilter(null)">全部</el-button>
            <el-button :type="groupStatus === 0    ? 'primary' : ''" size="small" @click="setFilter(0)">组外</el-button>
            <el-button :type="groupStatus === 1    ? 'primary' : ''" size="small" @click="setFilter(1)">组内</el-button>
          </el-button-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" stripe>
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="用户名" prop="username" min-width="130" />
        <el-table-column label="真实姓名" min-width="100" show-overflow-tooltip>
          <template slot-scope="{ row }">
            {{ row.nickname || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="邮箱" prop="email" min-width="160" />
        <el-table-column label="角色" width="100">
          <template slot-scope="{ row }">
            <el-tag :type="(roleMap[row.roleId] && roleMap[row.roleId].code) === 'leader' ? 'warning' : 'info'" size="mini">{{ roleLabel(row.roleId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="组状态" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.groupStatus === 1 ? 'success' : 'info'" size="mini">
              {{ row.groupStatus === 1 ? '组内' : '组外' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="mini">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" width="170" />
        <el-table-column label="余额（元）" width="120" align="right">
          <template slot-scope="{ row }">
            <span v-if="row.moneyQuota == null" style="color:#909399">未设置</span>
            <span v-else :style="{ color: row.moneyQuota <= 0 ? '#F56C6C' : '#303133' }">
              ¥{{ fmt(row.moneyQuota) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" @click="openRecharge(row)">充值</el-button>
            <el-button type="text" @click="openRoleEdit(row)">改角色</el-button>
            <el-button type="text" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-popconfirm title="确认冻结此用户？冻结后可联系管理员解冻恢复" @confirm="del(row.id)">
              <el-button slot="reference" type="text" style="color:#F56C6C;margin-left:8px">冻结</el-button>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:14px;text-align:right">
        <el-pagination
          :current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="p => { pageNum = p; load() }"
        />
      </div>
    </el-card>

    <!-- 新建账号 -->
    <el-dialog title="新建账号" :visible.sync="showCreate" width="440px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="form.nickname" placeholder="选填" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" show-password placeholder="初始密码" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="可选" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleId" style="width:100%">
            <el-option
              v-for="r in roleList.filter(r => r.code === 'leader' || r.code === 'member')"
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

    <!-- 充值弹窗 -->
    <el-dialog title="充值/扣减余额" :visible.sync="showRecharge" width="400px">
      <el-form label-width="110px">
        <el-form-item label="用户">
          <span style="font-weight:600">{{ rechargeForm.username }}</span>
          <span style="margin-left:10px;color:#909399;font-size:13px">
            当前余额：¥{{ fmt(rechargeForm.current) }}
          </span>
        </el-form-item>
        <el-form-item label="操作">
          <el-radio-group v-model="rechargeForm.mode">
            <el-radio label="add">充值（增加）</el-radio>
            <el-radio label="sub">扣减（减少）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额（元）">
          <el-input-number v-model="rechargeForm.amount" :min="0.01" :precision="2" style="width:160px" />
        </el-form-item>
        <el-form-item label="操作后余额">
          <span v-if="rechargeForm.mode === 'add'" style="color:#67C23A;font-weight:500">
            ¥{{ fmt((rechargeForm.current || 0) + (rechargeForm.amount || 0)) }}
          </span>
          <span v-else style="color:#F56C6C;font-weight:500">
            ¥{{ fmt(Math.max(0, (rechargeForm.current || 0) - (rechargeForm.amount || 0))) }}
          </span>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showRecharge = false">取消</el-button>
        <el-button type="primary" :loading="rechargeSaving" @click="doRecharge">确认</el-button>
      </div>
    </el-dialog>

    <!-- 改角色 -->
    <el-dialog title="修改角色" :visible.sync="showRole" width="320px">
      <el-select v-model="newRoleId" style="width:100%">
        <el-option
          v-for="r in roleList.filter(r => r.code === 'leader' || r.code === 'member')"
          :key="r.id"
          :value="r.id"
          :label="r.name"
        />
      </el-select>
      <div slot="footer">
        <el-button @click="showRole = false">取消</el-button>
        <el-button type="primary" :loading="roleSaving" @click="doChangeRole">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getOutGroupUsers, createAdminUser, updateAdminStatus, deleteAdminUser, rechargeAdminUser, setOutGroupUserRole } from '@/api/adminUser'
import { getRoleList } from '@/api/role'

export default {
  name: 'PortalAccounts',
  data() {
    return {
      list: [], total: 0, pageNum: 1, pageSize: 15,
      search: '', groupStatus: null,
      roleList: [], roleMap: {},
      showCreate: false, saving: false,
      form: { username: '', nickname: '', password: '', email: '', roleId: null },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }]
      },
      showRecharge: false, rechargeSaving: false,
      rechargeForm: { userId: null, username: '', current: 0, mode: 'add', amount: null },
      showRole: false, roleSaving: false,
      editId: null, newRoleId: null
    }
  },
  created() { this.init(); this.load() },
  methods: {
    async init() {
      try {
        this.roleList = await getRoleList()
        this.roleMap = {}
        this.roleList.forEach(r => { this.roleMap[r.id] = r })
        const member = this.roleList.find(r => r.code === 'member')
        if (member) this.form.roleId = member.id
      } catch { /* ignore */ }
    },
  methods: {
    fmt(val) {
      if (val == null) return '0.00'
      return Number(val).toFixed(2)
    },
    openRecharge(row) {
      this.rechargeForm = {
        userId: row.id,
        username: row.username,
        current: row.moneyQuota == null ? 0 : Number(row.moneyQuota),
        mode: 'add',
        amount: null
      }
      this.showRecharge = true
    },
    async doRecharge() {
      if (!this.rechargeForm.amount) return this.$message.warning('请输入金额')
      const delta = this.rechargeForm.mode === 'add' ? this.rechargeForm.amount : -this.rechargeForm.amount
      this.rechargeSaving = true
      try {
        await rechargeAdminUser(this.rechargeForm.userId, delta)
        this.$message.success('余额已' + (this.rechargeForm.mode === 'add' ? '充值' : '扣减'))
        this.showRecharge = false
        this.load()
      } finally { this.rechargeSaving = false }
    },
    setFilter(val) {
      this.groupStatus = val
      this.pageNum = 1
      this.load()
    },
    async load() {
      const res = await getOutGroupUsers({
        pageNum: this.pageNum,
        pageSize: this.pageSize,
        username: this.search || undefined,
        groupStatus: this.groupStatus != null ? this.groupStatus : undefined
      })
      this.list  = res.records
      this.total = res.total
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
        await createAdminUser(this.form)
        this.$message.success('账号创建成功')
        this.showCreate = false
        this.load()
      } finally { this.saving = false }
    },
    async toggleStatus(row) {
      const newStatus = row.status === 1 ? 0 : 1
      await updateAdminStatus(row.id, newStatus)
      this.$message.success(newStatus === 1 ? '已启用' : '已禁用')
      this.load()
    },
    async del(id) {
      await deleteAdminUser(id)
      this.$message.success('已冻结')
      this.load()
    },
    roleLabel(roleId) {
      const r = this.roleMap[roleId]
      return r ? r.name : '组员'
    },
    openRoleEdit(row) {
      this.editId  = row.id
      this.newRoleId = row.roleId
      this.showRole = true
    },
    async doChangeRole() {
      this.roleSaving = true
      try {
        await setOutGroupUserRole(this.editId, this.newRoleId)
        this.$message.success('角色已更新，用户已设为组外')
        this.showRole = false
        this.load()
      } finally { this.roleSaving = false }
    }
  }
}
</script>

<style scoped>
.page-title { font-size: 18px; font-weight: 600; color: #303133; margin-bottom: 20px; }
</style>
