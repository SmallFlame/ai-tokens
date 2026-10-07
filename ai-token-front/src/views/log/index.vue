<template>
  <div class="page-card">
    <!-- 筛选表单 -->
    <el-form :inline="true" :model="query" class="toolbar" @submit.native.prevent="search">
      <el-form-item v-if="isSuperAdmin">
        <el-select
          v-model="query.userId"
          placeholder="用户"
          clearable
          filterable
          style="width:140px"
        >
          <el-option
            v-for="user in userList"
            :key="user.id"
            :label="user.username"
            :value="user.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-input v-model="query.modelName" placeholder="模型名" clearable style="width:140px" />
      </el-form-item>
      <el-form-item>
        <el-select v-model="query.status" placeholder="状态" clearable style="width:110px">
          <el-option :value="1" label="成功" />
          <el-option :value="0" label="失败" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="yyyy-MM-dd HH:mm:ss"
          style="width:440px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="search">查询</el-button>
        <el-button icon="el-icon-refresh" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table :data="list" border stripe v-loading="loading" @row-click="showDetail">
      <el-table-column prop="createdAt"         label="调用时间"   min-width="220" />
      <el-table-column prop="userName"          label="用户"       min-width="120" show-overflow-tooltip />
      <el-table-column prop="clientIp"          label="IP"         min-width="140" show-overflow-tooltip />
      <el-table-column prop="modelName"         label="请求模型"   min-width="130" show-overflow-tooltip />
      <el-table-column prop="realModel"         label="实际模型"   min-width="130" show-overflow-tooltip />
      <el-table-column prop="channelName"       label="渠道"       min-width="120" show-overflow-tooltip />
      <el-table-column prop="promptTokens"       label="输入Token" min-width="80" align="right" />
      <el-table-column prop="cacheCreationTokens" label="缓存创建" min-width="80" align="right" />
      <el-table-column prop="cacheReadTokens"     label="缓存命中" min-width="80" align="right" />
      <el-table-column prop="completionTokens"    label="输出Token" min-width="80" align="right" />
      <el-table-column prop="totalTokens"         label="总计"    min-width="75" align="right" />
      <el-table-column prop="cost"              label="费用(元)"   min-width="90" align="right">
        <template slot-scope="{ row }">{{ row.cost || '0.000000' }}</template>
      </el-table-column>
      <el-table-column prop="durationMs"        label="耗时(ms)"   min-width="90" align="right" />
      <el-table-column label="流式" width="60" align="center">
        <template slot-scope="{ row }">
          <el-tag :type="row.isStream ? 'warning' : 'info'" size="mini">
            {{ row.isStream ? 'SSE' : '同步' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="70" align="center">
        <template slot-scope="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="mini">
            {{ row.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrap">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.pageNum"
        :page-sizes="[20, 50, 100]"
        @current-change="p => { query.pageNum = p; loadList() }"
        @size-change="s => { query.pageSize = s; query.pageNum = 1; loadList() }"
      />
    </div>

    <!-- 详情弹窗 -->
    <el-dialog title="调用详情" :visible.sync="detailVisible" width="600px">
      <el-descriptions :column="2" border size="small" v-if="detail">
        <el-descriptions-item label="请求ID">{{ detail.requestId }}</el-descriptions-item>
        <el-descriptions-item label="调用时间">{{ detail.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ detail.userName }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ detail.clientIp || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模型">{{ detail.modelName }}</el-descriptions-item>
        <el-descriptions-item label="实际模型">{{ detail.realModel }}</el-descriptions-item>
        <el-descriptions-item label="渠道">{{ detail.channelName }}</el-descriptions-item>
        <el-descriptions-item label="是否流式">{{ detail.isStream ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="输入 Token">{{ detail.promptTokens }}</el-descriptions-item>
        <el-descriptions-item label="输出 Token">{{ detail.completionTokens }}</el-descriptions-item>
        <el-descriptions-item label="缓存创建 Token">{{ detail.cacheCreationTokens || 0 }}</el-descriptions-item>
        <el-descriptions-item label="缓存命中 Token">{{ detail.cacheReadTokens || 0 }}</el-descriptions-item>
        <el-descriptions-item label="总 Token">{{ detail.totalTokens }}</el-descriptions-item>
        <el-descriptions-item label="费用(元)">{{ detail.cost }}</el-descriptions-item>
        <el-descriptions-item label="耗时(ms)">{{ detail.durationMs }}</el-descriptions-item>
        <el-descriptions-item label="HTTP 状态">{{ detail.httpStatus }}</el-descriptions-item>
        <el-descriptions-item label="状态" :span="2">
          <el-tag :type="detail.status === 1 ? 'success' : 'danger'" size="mini">
            {{ detail.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.errorMsg" label="错误信息" :span="2">
          <span style="color:#F56C6C">{{ detail.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getLogList, getLogDetail } from '@/api/log'
import { getUserList } from '@/api/user'

export default {
  name: 'LogPage',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      userList: [],
      dateRange: null,
      query: { pageNum: 1, pageSize: 20, userId: null, modelName: '', status: null, startTime: null, endTime: null },
      detailVisible: false,
      detail: null
    }
  },
  computed: {
    ...mapGetters(['isSuperAdmin', 'userInfo'])
  },
  created() {
    // 普通用户只能查看自己的日志
    if (!this.isSuperAdmin && this.userInfo) {
      this.query.userId = this.userInfo.id
    }
    this.loadUsers()
    this.loadList()
  },
  methods: {
    async loadUsers() {
      try {
        const page = await getUserList({ pageNum: 1, pageSize: 9999 })
        this.userList = page.records
      } catch (e) {
        console.error('获取用户列表失败', e)
      }
    },
    async loadList() {
      this.loading = true
      try {
        const params = {
          ...this.query,
          startTime: this.dateRange && this.dateRange[0],
          endTime:   this.dateRange && this.dateRange[1]
        }
        const page = await getLogList(params)
        this.list  = page.records
        this.total = page.total
      } finally { this.loading = false }
    },
    search() { this.query.pageNum = 1; this.loadList() },
    reset() {
      this.query = { pageNum: 1, pageSize: 20, userId: this.isSuperAdmin ? null : this.userInfo.id, modelName: '', status: null, startTime: null, endTime: null }
      this.dateRange = null
      this.loadList()
    },
    async showDetail(row) {
      this.detail = await getLogDetail(row.id)
      this.detailVisible = true
    }
  }
}
</script>

<style scoped>
.pagination-wrap { margin-top: 16px; text-align: right; }
</style>
