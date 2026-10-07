<template>
  <div>
    <div class="page-title">调用记录</div>

    <el-card shadow="never">
      <!-- 筛选栏 -->
      <el-form inline size="small" style="margin-bottom:12px">
        <el-form-item label="模型">
          <el-input v-model="query.modelName" placeholder="如: gpt-4o" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width:100px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <!-- <el-form-item label="时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="yyyy-MM-dd HH:mm:ss"
            :default-time="['00:00:00','23:59:59']"
            style="width:360px"
          />
        </el-form-item> -->
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" stripe >
        <el-table-column label="时间" prop="createdAt" min-width="185" />
        <el-table-column label="请求模型" prop="modelName" min-width="160" show-overflow-tooltip />
        <el-table-column label="实际模型" prop="realModel" min-width="160" show-overflow-tooltip />
        <el-table-column label="IP" prop="clientIp" min-width="140" show-overflow-tooltip />
        <el-table-column label="输入" prop="promptTokens" width="80" align="right" />
        <el-table-column label="缓存创建" prop="cacheCreationTokens" width="85" align="right" />
        <el-table-column label="缓存命中" prop="cacheReadTokens" width="85" align="right" />
        <el-table-column label="输出" prop="completionTokens" width="80" align="right" />
        <el-table-column label="总计" prop="totalTokens" width="80" align="right" />
        <el-table-column label="费用(元)" prop="cost" width="100" align="right" />
        <el-table-column label="耗时(ms)" prop="durationMs" width="90" align="right" />
        <el-table-column label="状态" width="70">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="mini">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="流式" width="60">
          <template slot-scope="{ row }">
            <el-tag v-if="row.isStream === 1" type="info" size="mini">流式</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:14px;text-align:right">
        <el-pagination
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="p => { query.pageNum = p; load() }"
        />
      </div>
    </el-card>
  </div>
</template>

<script>
import { getMyLogs } from '@/api/my'

export default {
  name: 'PortalMyLogs',
  data() {
    return {
      list: [],
      total: 0,
      dateRange: [],
      query: { pageNum: 1, pageSize: 15, modelName: '', status: null, startTime: null, endTime: null }
    }
  },
  created() { this.load() },
  methods: {
    async load() {
      const q = { ...this.query }
      if (this.dateRange && this.dateRange.length === 2) {
        q.startTime = this.dateRange[0]
        q.endTime   = this.dateRange[1]
      }
      const res = await getMyLogs(q)
      this.list  = res.records
      this.total = res.total
    },
    search() { this.query.pageNum = 1; this.load() },
    reset() {
      this.query = { pageNum: 1, pageSize: 15, modelName: '', status: null, startTime: null, endTime: null }
      this.dateRange = []
      this.load()
    }
  }
}
</script>

<style scoped>
.page-title { font-size: 18px; font-weight: 600; color: #303133; margin-bottom: 20px; }
</style>
