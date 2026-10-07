<template>
  <div class="page-card">
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-input v-model="searchName" placeholder="小组名称" clearable style="width:200px" />
      <el-button type="primary" icon="el-icon-search" @click="loadList">查询</el-button>
      <el-button icon="el-icon-refresh" @click="searchName = ''; loadList()">重置</el-button>
      <el-button type="success" icon="el-icon-plus" style="margin-left:auto" @click="openCreate">新建小组</el-button>
    </div>

    <el-table :data="list" border stripe v-loading="loading">
      <el-table-column prop="id"          label="ID"    width="70" />
      <el-table-column prop="name"        label="小组名称" min-width="130" />
      <el-table-column prop="remark"      label="备注"   min-width="160" show-overflow-tooltip />
      <el-table-column prop="leaderName"  label="组长"   width="110" />
      <el-table-column label="周报" width="90" align="center">
        <template slot-scope="{ row }">
          <el-tag :type="row.reportType === 0 ? 'info' : 'success'" size="small">
            {{ row.reportType === 0 ? '不需交' : '需交' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="memberCount" label="成员数" width="80" align="center" />
      <el-table-column label="小组金额池" width="120" align="right">
        <template slot-scope="{ row }">
          <span>¥{{ fmt(row.moneyPool) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="成员余额总计" width="120" align="right">
        <template slot-scope="{ row }">
          <span>¥{{ fmt(row.totalMoney) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt"   label="创建时间" min-width="180" />
      <el-table-column label="操作" width="210" fixed="right">
        <template slot-scope="{ row }">
          <el-button type="text" @click="openEdit(row)">编辑</el-button>
          <el-button type="text" @click="openMembers(row)">成员</el-button>
          <el-button type="text" style="color:#409EFF" @click="openRechargePool(row)">充值金额池</el-button>
          <el-popconfirm title="确认删除该小组？" @confirm="del(row.id)">
            <el-button slot="reference" type="text" style="color:#F56C6C">删除</el-button>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建/编辑对话框 -->
    <el-dialog :title="editId ? '编辑小组' : '新建小组'" :visible.sync="showForm" width="480px" @close="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="小组名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入小组名称" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
        <el-form-item label="组长" prop="leaderId">
          <el-select v-model="form.leaderId" placeholder="请选择组长（role=1 的用户）" style="width:100%" filterable>
            <el-option
              v-for="u in leaderOptions"
              :key="u.id"
              :label="u.username"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="周报">
          <el-select v-model="form.reportType" style="width:100%">
            <el-option :value="1" label="需交周报" />
            <el-option :value="0" label="不需交" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doSave">保存</el-button>
      </div>
    </el-dialog>

    <!-- 成员管理对话框 -->
    <el-dialog :title="'成员管理 - ' + (currentGroup && currentGroup.name)" :visible.sync="showMembers" width="640px">
      <div style="display:flex;justify-content:flex-end;margin-bottom:12px">
        <el-select
          v-model="selectedUserId"
          placeholder="选择未分组用户加入"
          filterable
          style="width:220px;margin-right:8px"
        >
          <el-option v-for="u in availableUsers" :key="u.id" :label="u.username" :value="u.id" />
        </el-select>
        <el-button type="primary" size="small" @click="doAddMember" :disabled="!selectedUserId">加入小组</el-button>
      </div>
      <el-table :data="memberList" stripe v-loading="memberLoading" size="small">
        <el-table-column prop="id"       label="ID"   width="70" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="真实姓名" min-width="100" show-overflow-tooltip>
          <template slot-scope="{ row }">
            {{ row.nickname || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="email"    label="邮箱"  min-width="160" show-overflow-tooltip />
        <el-table-column label="角色" width="70">
          <template slot-scope="{ row }">
            <el-tag size="mini" :type="row.id === currentGroup.leaderId ? 'warning' : 'info'">
              {{ row.id === currentGroup.leaderId ? '组长' : '组员' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="余额（元）" width="120" align="right">
          <template slot-scope="{ row }">
            <span v-if="row.moneyQuota == null" style="color:#909399">未设置</span>
            <span v-else :style="{ color: row.moneyQuota <= 0 ? '#F56C6C' : '#303133' }">
              ¥{{ fmt(row.moneyQuota) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template slot-scope="{ row }">
            <el-popconfirm
              v-if="row.id !== currentGroup.leaderId"
              title="确认将此用户移出小组？"
              @confirm="doRemoveMember(row.id)"
            >
              <el-button slot="reference" type="text" style="color:#F56C6C">移出</el-button>
            </el-popconfirm>
            <span v-else style="color:#E6A23C;font-size:12px">组长</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
    <!-- 充值金额池对话框 -->
    <el-dialog title="充值小组金额池" :visible.sync="showRechargePool" width="380px" @close="resetRechargePool">
      <el-form label-width="100px">
        <el-form-item label="小组">
          <span style="font-weight:600">{{ rechargePoolForm.groupName }}</span>
        </el-form-item>
        <el-form-item label="组长">
          <span>{{ rechargePoolForm.leaderName }}</span>
        </el-form-item>
        <el-form-item label="当前金额池">
          <span style="color:#67C23A;font-weight:600">¥{{ fmt(rechargePoolForm.currentPool) }}</span>
        </el-form-item>
        <el-form-item label="充值金额（元）">
          <el-input-number
            v-model="rechargePoolForm.amount"
            :min="0.01"
            :precision="2"
            style="width:160px"
          />
        </el-form-item>
        <el-form-item label="充值后">
          <span style="color:#409EFF;font-weight:600">
            ¥{{ fmt((Number(rechargePoolForm.currentPool) || 0) + (Number(rechargePoolForm.amount) || 0)) }}
          </span>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showRechargePool = false">取消</el-button>
        <el-button type="primary" :loading="rechargePoolSaving" @click="doRechargePool">确认充值</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import { getGroupList, createGroup, updateGroup, deleteGroup, getGroupMembers, addGroupMember, removeGroupMember, rechargeGroupMoneyPool } from '@/api/group'
import { getOutGroupUsers } from '@/api/adminUser'
import { getRoleList } from '@/api/role'

export default {
  name: 'GroupPage',
  data() {
    return {
      loading: false,
      roleMap: {},
      list: [],
      searchName: '',
      // 新建/编辑
      showForm: false,
      saving: false,
      editId: null,
      form: { name: '', remark: '', leaderId: null, reportType: 1 },
      rules: {
        name:     [{ required: true, message: '请输入小组名称', trigger: 'blur' }],
        leaderId: [{ required: true, message: '请选择组长',     trigger: 'change' }]
      },
      leaderOptions: [],
      // 成员管理
      showMembers: false,
      memberLoading: false,
      currentGroup: null,
      memberList: [],
      availableUsers: [],
      selectedUserId: null,
      // 充值金额池
      showRechargePool: false,
      rechargePoolSaving: false,
      rechargePoolForm: { groupId: null, groupName: '', leaderName: '', currentPool: 0, amount: null }
    }
  },
  async created() {
    try {
      const roles = await getRoleList()
      this.roleMap = {}
      roles.forEach(r => { this.roleMap[r.id] = r })
    } catch { /* ignore */ }
    this.loadList()
    this.loadLeaders()
  },
  methods: {
    fmt(val) {
      if (val == null) return '0.00'
      return Number(val).toFixed(2)
    },
    async loadList() {
      this.loading = true
      try { this.list = await getGroupList(this.searchName || undefined) }
      finally { this.loading = false }
    },
    async loadLeaders() {
      const page = await getOutGroupUsers({ pageNum: 1, pageSize: 999 })
      this.leaderOptions = (page.records || []).filter(u => {
        const r = this.roleMap[u.roleId]
        return r && r.code === 'leader'
      })
    },
    openCreate() {
      this.editId = null
      this.form = { name: '', remark: '', leaderId: null, reportType: 1 }
      this.showForm = true
    },
    openEdit(row) {
      this.editId = row.id
      this.form = { name: row.name, remark: row.remark || '', leaderId: row.leaderId, reportType: row.reportType == null ? 1 : row.reportType }
      this.showForm = true
    },
    resetForm() {
      this.$refs.formRef && this.$refs.formRef.resetFields()
    },
    async doSave() {
      try { await this.$refs.formRef.validate() } catch { return }
      this.saving = true
      try {
        if (this.editId) {
          await updateGroup(this.editId, this.form)
        } else {
          await createGroup(this.form)
        }
        this.$message.success(this.editId ? '修改成功' : '创建成功')
        this.showForm = false
        this.loadList()
      } finally {
        this.saving = false
      }
    },
    async del(id) {
      await deleteGroup(id)
      this.$message.success('已删除')
      this.loadList()
    },
    async openMembers(row) {
      this.currentGroup = row
      this.showMembers = true
      this.selectedUserId = null
      await Promise.all([this.loadMembers(row.id), this.loadAvailableUsers()])
    },
    async loadMembers(groupId) {
      this.memberLoading = true
      try { this.memberList = await getGroupMembers(groupId) }
      finally { this.memberLoading = false }
    },
    async loadAvailableUsers() {
      const page = await getOutGroupUsers({ pageNum: 1, pageSize: 999, groupStatus: 0 })
      this.availableUsers = (page.records || []).filter(u => {
        const r = this.roleMap[u.roleId]
        return r && r.code === 'member'
      })
    },
    async doAddMember() {
      if (!this.selectedUserId) return
      await addGroupMember(this.currentGroup.id, this.selectedUserId)
      this.$message.success('已加入小组')
      this.selectedUserId = null
      await Promise.all([this.loadMembers(this.currentGroup.id), this.loadAvailableUsers(), this.loadList()])
    },
    async doRemoveMember(userId) {
      await removeGroupMember(this.currentGroup.id, userId)
      this.$message.success('已移出小组')
      await Promise.all([this.loadMembers(this.currentGroup.id), this.loadAvailableUsers(), this.loadList()])
    },
    // 充值金额池
    openRechargePool(row) {
      this.rechargePoolForm = {
        groupId: row.id,
        groupName: row.name,
        leaderName: row.leaderName || '-',
        currentPool: Number(row.moneyPool) || 0,
        amount: null
      }
      this.showRechargePool = true
    },
    resetRechargePool() {
      this.rechargePoolForm = { groupId: null, groupName: '', leaderName: '', currentPool: 0, amount: null }
    },
    async doRechargePool() {
      if (!this.rechargePoolForm.amount || this.rechargePoolForm.amount <= 0) {
        return this.$message.warning('请输入充值金额')
      }
      if (!this.rechargePoolForm.groupId) {
        return this.$message.warning('小组信息错误，无法充值')
      }
      this.rechargePoolSaving = true
      try {
        await rechargeGroupMoneyPool(this.rechargePoolForm.groupId, this.rechargePoolForm.amount)
        this.$message.success('充值成功')
        this.showRechargePool = false
        this.loadList()
      } finally { this.rechargePoolSaving = false }
    }
  }
}
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
</style>
