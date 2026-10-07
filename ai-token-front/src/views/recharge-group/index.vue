<template>
  <div class="page-card">
    <div class="page-title">小组充值日志</div>

    <div class="toolbar">
      <el-input v-model="query.groupName" placeholder="小组名称" clearable style="width:180px" @keyup.enter.native="search" />
      <el-select v-model="query.type" placeholder="类型" clearable style="width:140px">
        <el-option value="recharge" label="管理员充值" />
        <el-option value="allocate" label="分给组员" />
        <el-option value="return" label="组员归还" />
      </el-select>
      <el-button type="primary" icon="el-icon-search" @click="search">查询</el-button>
      <el-button icon="el-icon-refresh" @click="reset">重置</el-button>
    </div>

    <el-table :data="list" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="groupName" label="小组名称" width="140" />
      <el-table-column label="类型" width="110">
        <template slot-scope="{ row }">
          <el-tag :type="typeTag(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="变动金额" width="130" align="right">
        <template slot-scope="{ row }">
          <span :style="{ color: row.amount >= 0 ? '#67C23A' : '#F56C6C', fontWeight: '500' }">
            {{ row.amount >= 0 ? '+' : '' }}{{ fmt(row.amount) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="变动前金额池" width="140" align="right">
        <template slot-scope="{ row }">¥{{ fmt(row.beforeBalance) }}</template>
      </el-table-column>
      <el-table-column label="变动后金额池" width="140" align="right">
        <template slot-scope="{ row }">¥{{ fmt(row.afterBalance) }}</template>
      </el-table-column>
      <el-table-column prop="operatorName" label="操作人" width="120" />
      <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip>
        <template slot-scope="{ row }">{{ row.remark || '-' }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" width="170" />
    </el-table>

    <div class="pagination">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        :current-page.sync="query.pageNum"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script>
import { getGroupRechargeLogs } from '@/api/rechargeLog'

const TYPE_MAP = { recharge: '管理员充值', allocate: '分给组员', return: '组员归还' }

export default {
  name: 'RechargeGroupPage',
  data() {
    return {
      loading: false, list: [], total: 0,
      query: { pageNum: 1, pageSize: 20, groupName: '', type: '' }
    }
  },
  created() { this.load() },
  methods: {
    fmt(v) { return v != null ? Number(v).toFixed(4) : '0.0000' },
    typeLabel(t) { return TYPE_MAP[t] || t },
    typeTag(t) {
      if (t === 'recharge') return 'success'
      if (t === 'allocate') return 'warning'
      if (t === 'return') return 'primary'
      return 'info'
    },
    async load() {
      this.loading = true
      try {
        const res = await getGroupRechargeLogs({
          pageNum: this.query.pageNum, pageSize: this.query.pageSize,
          groupName: this.query.groupName || undefined,
          type: this.query.type || undefined
        })
        this.list = res.records || []
        this.total = res.total || 0
      } finally { this.loading = false }
    },
    search() { this.query.pageNum = 1; this.load() },
    reset() { this.query = { pageNum: 1, pageSize: 20, groupName: '', type: '' }; this.load() }
  }
}
</script>

<style scoped>
.page-title { font-size: 18px; font-weight: 600; color: #303133; margin-bottom: 16px; }
.toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
.pagination { margin-top: 16px; text-align: right; }
</style>
