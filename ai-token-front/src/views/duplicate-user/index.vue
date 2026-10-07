<template>
  <div class="page-card">
    <el-tabs v-model="activeTab" @tab-click="onTabChange">
      <!-- ══════════ 重复账号 ══════════ -->
      <el-tab-pane label="重复账号" name="duplicates">
        <div class="toolbar">
          <el-input
            v-model="keyword"
            placeholder="学号 / 邮箱 / 用户名"
            clearable
            style="width:240px"
            @keyup.enter.native="load"
          />
          <el-button type="primary" icon="el-icon-search" @click="load">查询</el-button>
          <el-button icon="el-icon-refresh" @click="reset">重置</el-button>
          <span class="summary">
            共 <strong>{{ list.length }}</strong> 组重复账号
          </span>
        </div>

        <div v-loading="loading">
          <el-empty v-if="!loading && list.length === 0" description="未发现重复账号" />

          <div v-for="(group, gi) in list" :key="gi" class="group-card">
            <div class="group-head">
              <i class="el-icon-warning" />
              <span class="group-title">
                重复依据：
                <el-tag
                  v-for="f in group.matchedFields"
                  :key="f"
                  size="mini"
                  type="warning"
                  style="margin-right:6px"
                >
                  {{ fieldLabel(f, group) }}
                </el-tag>
              </span>
              <span class="group-count">{{ group.users.length }} 个账号</span>
            </div>

            <el-table :data="group.users" border stripe size="small">
              <el-table-column prop="id"       label="ID"     width="70" />
              <el-table-column prop="username" label="用户名" min-width="120" />
              <el-table-column label="真实姓名" min-width="100">
                <template slot-scope="{ row }">{{ row.nickname || '-' }}</template>
              </el-table-column>
              <el-table-column label="学号/工号" min-width="130">
                <template slot-scope="{ row }">{{ row.studentId || '-' }}</template>
              </el-table-column>
              <el-table-column label="邮箱" min-width="180" show-overflow-tooltip>
                <template slot-scope="{ row }">{{ row.email || '-' }}</template>
              </el-table-column>
              <el-table-column label="角色" width="90">
                <template slot-scope="{ row }">
                  <el-tag :type="roleTag(row.roleId)" size="mini">{{ row.roleName || '-' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="小组" min-width="110">
                <template slot-scope="{ row }">{{ row.groupName || '组外' }}</template>
              </el-table-column>
              <el-table-column label="余额（元）" width="110" align="right">
                <template slot-scope="{ row }">
                  <span v-if="row.moneyQuota == null" style="color:#909399">未设置</span>
                  <span v-else>¥{{ fmt(row.moneyQuota) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="80" align="center">
                <template slot-scope="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="mini">
                    {{ row.status === 1 ? '启用' : '禁用' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>

      <!-- ══════════ 已冻结账号 ══════════ -->
      <el-tab-pane label="已冻结账号" name="deleted">
        <div class="toolbar">
          <el-input
            v-model="frozenSearch"
            placeholder="用户名"
            clearable
            style="width:200px"
            @keyup.enter.native="loadFrozen"
          />
          <el-button type="primary" icon="el-icon-search" @click="loadFrozen">查询</el-button>
          <el-button icon="el-icon-refresh" @click="resetFrozen">重置</el-button>
          <span class="summary">
            共 <strong>{{ frozenTotal }}</strong> 个冻结账号
          </span>
        </div>

        <el-table :data="frozenList" border stripe v-loading="frozenLoading">
          <el-table-column prop="id"       label="ID"     width="70" />
          <el-table-column prop="username" label="用户名" min-width="120" />
          <el-table-column label="真实姓名" min-width="100">
            <template slot-scope="{ row }">{{ row.nickname || '-' }}</template>
          </el-table-column>
          <el-table-column label="学号/工号" min-width="130">
            <template slot-scope="{ row }">{{ row.studentId || '-' }}</template>
          </el-table-column>
          <el-table-column label="邮箱" min-width="180" show-overflow-tooltip>
            <template slot-scope="{ row }">{{ row.email || '-' }}</template>
          </el-table-column>
          <el-table-column label="余额（元）" width="110" align="right">
            <template slot-scope="{ row }">
              <span v-if="row.moneyQuota == null" style="color:#909399">未设置</span>
              <span v-else>¥{{ fmt(row.moneyQuota) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right">
            <template slot-scope="{ row }">
              <el-popconfirm title="确认解冻该账号？" @confirm="doRestore(row.id)">
                <el-button slot="reference" type="text">解冻</el-button>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination">
          <el-pagination
            background
            layout="total, prev, pager, next"
            :total="frozenTotal"
            :page-size="frozenPageSize"
            :current-page.sync="frozenPageNum"
            @current-change="loadFrozen"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { getDuplicateUsers, getDeletedUsers, restoreUser } from '@/api/adminUser'
import { getRoleList } from '@/api/role'

export default {
  name: 'DuplicateUserPage',
  data() {
    return {
      activeTab: 'duplicates',
      loading: false,
      list: [],
      keyword: '',
      roleMap: {},
      // 已冻结账号
      frozenLoading: false,
      frozenList: [],
      frozenTotal: 0,
      frozenPageNum: 1,
      frozenPageSize: 20,
      frozenSearch: ''
    }
  },
  created() {
    this.init()
    this.load()
  },
  methods: {
    async init() {
      try {
        const roles = await getRoleList()
        this.roleMap = {}
        roles.forEach(r => { this.roleMap[r.id] = r })
      } catch { /* ignore */ }
    },
    onTabChange(tab) {
      if (tab.name === 'deleted' && this.frozenList.length === 0 && !this.frozenLoading) {
        this.loadFrozen()
      }
    },
    async load() {
      this.loading = true
      try {
        this.list = await getDuplicateUsers({ keyword: this.keyword || undefined }) || []
      } finally {
        this.loading = false
      }
    },
    reset() {
      this.keyword = ''
      this.load()
    },
    async loadFrozen() {
      this.frozenLoading = true
      try {
        const res = await getDeletedUsers({
          pageNum: this.frozenPageNum,
          pageSize: this.frozenPageSize,
          username: this.frozenSearch || undefined
        })
        this.frozenList  = res.records || []
        this.frozenTotal = res.total   || 0
      } finally {
        this.frozenLoading = false
      }
    },
    resetFrozen() {
      this.frozenSearch = ''
      this.frozenPageNum = 1
      this.loadFrozen()
    },
    async doRestore(id) {
      await restoreUser(id)
      this.$message.success('账号已解冻')
      this.loadFrozen()
    },
    fmt(val) {
      if (val == null) return '0.00'
      return Number(val).toFixed(2)
    },
    fieldLabel(field, group) {
      if (field === 'studentId') return `学号 ${group.studentId}`
      if (field === 'email')     return `邮箱 ${group.email}`
      return field
    },
    roleTag(roleId) {
      const r = this.roleMap[roleId]
      const code = r ? r.code : ''
      if (code === 'super_admin') return 'danger'
      if (code === 'leader')      return 'warning'
      if (code === 'advisor')     return 'success'
      return 'info'
    }
  }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.summary {
  margin-left: auto;
  color: #909399;
  font-size: 13px;
}
.group-card {
  border: 1px solid #EBEEF5;
  border-radius: 6px;
  padding: 12px 16px;
  margin-bottom: 16px;
  background: #fff;
}
.group-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  color: #E6A23C;
  font-weight: 600;
}
.group-title {
  color: #303133;
  font-weight: 500;
}
.group-count {
  margin-left: auto;
  color: #909399;
  font-size: 13px;
  font-weight: normal;
}
</style>
