<template>
  <div class="page-card">
    <!-- 工具栏 -->
    <div class="toolbar">
      <span class="label">时间范围：</span>
      <el-radio-group v-model="days" size="small" @change="load">
        <el-radio-button :label="1">今天</el-radio-button>
        <el-radio-button :label="7">近 7 天</el-radio-button>
        <el-radio-button :label="30">近 30 天</el-radio-button>
        <el-radio-button :label="0">全部</el-radio-button>
      </el-radio-group>
      <el-input
        v-model="search"
        placeholder="搜索用户名"
        clearable
        size="small"
        prefix-icon="el-icon-search"
        style="width:180px; margin-left:16px"
      />
      <el-button style="margin-left:auto" size="small" icon="el-icon-refresh" @click="load">刷新</el-button>
    </div>

    <!-- 顶部汇总 -->
    <el-row :gutter="16" class="summary-row" v-if="list.length">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">活跃用户</div>
          <div class="stat-val">{{ activeUsers }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">总请求数</div>
          <div class="stat-val">{{ totalRequests.toLocaleString() }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">总 Tokens</div>
          <div class="stat-val">{{ totalTokens.toLocaleString() }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">总费用
            <el-tooltip
              placement="bottom"
              effect="light"
              content="费用由 tokens 折算，实际价格可能略有波动，以 tokens 消耗为准。">
              <i class="el-icon-question tip-icon" />
            </el-tooltip>
          </div>
          <div class="stat-val">¥ {{ totalCost }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 主表格 -->
    <el-table
      :data="filtered"
      border
      stripe
      v-loading="loading"
      :default-sort="{ prop: 'totalTokens', order: 'descending' }"
    >
      <el-table-column label="用户名" prop="username" min-width="130" sortable />

      <el-table-column label="总请求" prop="totalRequests" width="100" align="right" sortable>
        <template slot-scope="{ row }">{{ row.totalRequests.toLocaleString() }}</template>
      </el-table-column>

      <el-table-column label="成功" prop="successRequests" width="90" align="right" sortable>
        <template slot-scope="{ row }">
          <span style="color:#67C23A">{{ row.successRequests.toLocaleString() }}</span>
        </template>
      </el-table-column>

      <el-table-column label="失败" prop="failRequests" width="80" align="right" sortable>
        <template slot-scope="{ row }">
          <span :style="{ color: row.failRequests > 0 ? '#F56C6C' : '#909399' }">
            {{ row.failRequests.toLocaleString() }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="成功率" width="120" align="center" sortable :sort-method="(a,b) => rate(a) - rate(b)">
        <template slot-scope="{ row }">
          <template v-if="row.totalRequests > 0">
            <el-progress
              :percentage="rate(row)"
              :status="row.failRequests > 0 ? (rate(row) < 80 ? 'exception' : 'warning') : 'success'"
              :stroke-width="6"
              :show-text="false"
              style="width:60px;display:inline-block;vertical-align:middle;margin-right:6px"
            />
            <span style="font-size:12px">{{ rate(row) }}%</span>
          </template>
          <span v-else style="color:#C0C4CC">-</span>
        </template>
      </el-table-column>

      <el-table-column label="Token 消耗" prop="totalTokens" min-width="160" sortable>
        <template slot-scope="{ row }">
          <template v-if="row.totalTokens > 0">
            <el-progress
              :percentage="tokenPct(row)"
              color="#409EFF"
              :stroke-width="6"
              :show-text="false"
              style="margin-bottom:2px"
            />
            <span class="small-text">{{ row.totalTokens.toLocaleString() }}</span>
          </template>
          <span v-else class="small-text muted">0</span>
        </template>
      </el-table-column>

      <el-table-column label="费用 (¥)" prop="totalCost" width="110" align="right" sortable>
        <template slot-scope="{ row }">
          <span :style="{ color: row.totalCost > 0 ? '#303133' : '#C0C4CC' }">
            ¥ {{ Number(row.totalCost).toFixed(4) }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="均耗时" prop="avgDurationMs" width="100" align="right" sortable>
        <template slot-scope="{ row }">
          <span class="small-text">{{ row.avgDurationMs ? row.avgDurationMs + ' ms' : '-' }}</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import { getUserStat } from '@/api/stat'

export default {
  name: 'UserStat',
  data() {
    return {
      days: 7,
      loading: false,
      list: [],
      search: ''
    }
  },
  computed: {
    filtered() {
      if (!this.search) return this.list
      const kw = this.search.toLowerCase()
      return this.list.filter(r => r.username.toLowerCase().includes(kw))
    },
    maxTokens() {
      return Math.max(...this.list.map(r => r.totalTokens), 1)
    },
    activeUsers() {
      return this.list.filter(r => r.totalRequests > 0).length
    },
    totalRequests() {
      return this.list.reduce((s, r) => s + r.totalRequests, 0)
    },
    totalTokens() {
      return this.list.reduce((s, r) => s + r.totalTokens, 0)
    },
    totalCost() {
      return this.list.reduce((s, r) => s + Number(r.totalCost), 0).toFixed(4)
    }
  },
  created() {
    this.load()
  },
  methods: {
    async load() {
      this.loading = true
      try {
        this.list = await getUserStat(this.days)
      } finally {
        this.loading = false
      }
    },
    rate(row) {
      if (!row.totalRequests) return 0
      return Math.round(row.successRequests / row.totalRequests * 100)
    },
    tokenPct(row) {
      return Math.round(row.totalTokens / this.maxTokens * 100)
    }
  }
}
</script>

<style scoped>
.toolbar { display: flex; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 8px; }
.label { color: #606266; font-size: 14px; }

.summary-row { margin-bottom: 20px; }
.stat-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 16px 20px;
  text-align: center;
}
.stat-label { font-size: 13px; color: #909399; margin-bottom: 8px; }
.tip-icon { margin-left: 4px; color: #c0c4cc; cursor: default; font-size: 13px; vertical-align: middle; }
.stat-val { font-size: 22px; font-weight: 600; color: #303133; }

.small-text { font-size: 13px; color: #606266; }
.muted { color: #C0C4CC; }
</style>
