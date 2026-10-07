<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="search" placeholder="用户名" clearable style="width:200px" />
      <!-- <el-button-group>
        <el-button :type="groupStatus === null ? 'primary' : ''" @click="setFilter(null)">全部</el-button>
        <el-button :type="groupStatus === 0    ? 'primary' : ''" @click="setFilter(0)">组外</el-button>
        <el-button :type="groupStatus === 1    ? 'primary' : ''" @click="setFilter(1)">组内</el-button>
      </el-button-group> -->
      <el-button type="primary" icon="el-icon-search" @click="load">查询</el-button>
      <el-button icon="el-icon-refresh" @click="reset">重置</el-button>
    </div>

    <el-table :data="list" border stripe v-loading="loading">
      <el-table-column prop="id"       label="ID"   width="70" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="nickname" label="真实姓名" min-width="100" show-overflow-tooltip>
        <template slot-scope="{ row }">
          {{ row.nickname || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="email"    label="邮箱"  min-width="160" show-overflow-tooltip />
      <el-table-column label="角色" width="100">
        <template slot-scope="{ row }">
          <el-tag :type="roleTag(row.roleId)" size="small">{{ roleLabel(row.roleId) }}</el-tag>
        </template>
      </el-table-column>
      <!-- <el-table-column label="组状态" width="80" align="center">
        <template slot-scope="{ row }">
          <el-tag :type="row.groupStatus === 1 ? 'success' : 'info'" size="mini">
            {{ row.groupStatus === 1 ? '组内' : '组外' }}
          </el-tag>
        </template>
      </el-table-column> -->
      <el-table-column label="余额（元）" width="120" align="right">
        <template slot-scope="{ row }">
          <span v-if="row.moneyQuota == null" style="color:#909399">未设置</span>
          <span v-else :style="{ color: row.moneyQuota <= 0 ? '#F56C6C' : '#303133' }">
            ¥{{ fmt(row.moneyQuota) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template slot-scope="{ row }">
          <el-button type="text" @click="openRecharge(row)">充值</el-button>
          <el-button type="text" @click="openRoleEdit(row)">改角色</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page.sync="pageNum"
        @current-change="load"
      />
    </div>

    <!-- 改角色弹窗 -->
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
        <el-button type="primary" :loading="roleSaving" @click="doSaveRole">保存</el-button>
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
  </div>
</template>

<script>
import { getOutGroupUsers, rechargeAdminUser, setOutGroupUserRole } from '@/api/adminUser'
import { getRoleList } from '@/api/role'

export default {
  name: 'AdminUserPage',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 20,
      search: '',
      groupStatus: null,
      roleList: [],
      roleMap: {},
      showRole: false,
      roleSaving: false,
      editId: null,
      newRoleId: null,
      showRecharge: false,
      rechargeSaving: false,
      rechargeForm: { userId: null, username: '', current: 0, mode: 'add', amount: null }
    }
  },
  created() {
    this.init()
    this.load()
  },
  methods: {
    async init() {
      try {
        this.roleList = await getRoleList()
        this.roleMap = {}
        this.roleList.forEach(r => { this.roleMap[r.id] = r })
      } catch { /* ignore */ }
    },
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
    reset() {
      this.search = ''
      this.groupStatus = null
      this.pageNum = 1
      this.load()
    },
    async load() {
      this.loading = true
      try {
        const res = await getOutGroupUsers({
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          username: this.search || undefined,
          groupStatus: this.groupStatus != null ? this.groupStatus : undefined
        })
        this.list  = res.records || []
        this.total = res.total   || 0
      } finally {
        this.loading = false
      }
    },
    roleLabel(roleId) {
      const r = this.roleMap[roleId]
      return r ? r.name : '-'
    },
    roleTag(roleId) {
      const r = this.roleMap[roleId]
      const code = r ? r.code : ''
      if (code === 'super_admin') return 'danger'
      if (code === 'leader') return 'warning'
      if (code === 'advisor') return 'success'
      return 'info'
    },
    openRoleEdit(row) {
      this.editId  = row.id
      this.newRoleId = row.roleId
      this.showRole = true
    },
    async doSaveRole() {
      this.roleSaving = true
      try {
        await setOutGroupUserRole(this.editId, this.newRoleId)
        this.$message.success('角色已更新，用户已设为组外')
        this.showRole = false
        this.load()
      } finally {
        this.roleSaving = false
      }
    }
  }
}
</script>

<style scoped>
.toolbar    { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
