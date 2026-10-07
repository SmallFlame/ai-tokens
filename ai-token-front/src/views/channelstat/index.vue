<template>
  <div class="page-card">
    <!-- 时间筛选 -->
    <div class="toolbar">
      <span class="label">时间范围：</span>
      <el-radio-group v-model="days" size="small" @change="load">
        <el-radio-button :label="1">今天</el-radio-button>
        <el-radio-button :label="7">近 7 天</el-radio-button>
        <el-radio-button :label="30">近 30 天</el-radio-button>
        <el-radio-button :label="0">全部</el-radio-button>
      </el-radio-group>
      <el-button style="margin-left:12px" size="small" icon="el-icon-refresh" @click="load">刷新</el-button>
    </div>

    <!-- 无数据提示 -->
    <el-empty v-if="!loading && list.length === 0" description="暂无调用记录" />

    <template v-else>
      <!-- 汇总卡片 -->
      <el-row :gutter="16" class="cards">
        <el-col v-for="item in list" :key="item.channelId" :xs="24" :sm="12" :md="8" :lg="6">
          <el-card shadow="hover" class="channel-card">
            <div class="card-name">{{ item.channelName }}</div>
            <div class="card-rows">
              <div class="card-row">
                <span class="card-label">总请求</span>
                <span class="card-val">{{ item.totalRequests.toLocaleString() }}</span>
              </div>
              <div class="card-row">
                <span class="card-label">成功率</span>
                <span class="card-val" :style="{ color: rateColor(item) }">
                  {{ rate(item) }}%
                </span>
              </div>
              <div class="card-row">
                <span class="card-label">Tokens</span>
                <span class="card-val">{{ item.totalTokens.toLocaleString() }}</span>
              </div>
              <div class="card-row">
                <span class="card-label">费用</span>
                <span class="card-val" style="color:#E6A23C">¥ {{ Number(item.totalCost || 0).toFixed(4) }}</span>
              </div>
              <div class="card-row">
                <span class="card-label">均耗时</span>
                <span class="card-val">{{ item.avgDurationMs ? item.avgDurationMs + ' ms' : '-' }}</span>
              </div>
            </div>
            <el-progress
              :percentage="rate(item)"
              :status="item.failRequests > 0 ? 'exception' : 'success'"
              :stroke-width="6"
              :show-text="false"
              style="margin-top:10px"
            />
          </el-card>
        </el-col>
      </el-row>

      <!-- 图表区 -->
      <el-row :gutter="16" class="charts">
        <el-col :xs="24" :md="12">
          <el-card shadow="never">
            <div slot="header">请求量分布（饼图）</div>
            <div ref="pieChart" class="chart-box" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card shadow="never">
            <div slot="header">Token 消耗对比（柱状图）</div>
            <div ref="barChart" class="chart-box" />
          </el-card>
        </el-col>
      </el-row>

      <!-- 明细表 -->
      <el-card shadow="never" style="margin-top:16px">
        <div slot="header">明细数据</div>
        <el-table :data="list" border stripe>
          <el-table-column label="渠道" prop="channelName" min-width="140" />
          <el-table-column label="总请求" prop="totalRequests" width="100" align="right">
            <template slot-scope="{ row }">{{ row.totalRequests.toLocaleString() }}</template>
          </el-table-column>
          <el-table-column label="成功" prop="successRequests" width="90" align="right">
            <template slot-scope="{ row }">
              <span style="color:#67C23A">{{ row.successRequests.toLocaleString() }}</span>
            </template>
          </el-table-column>
          <el-table-column label="失败" prop="failRequests" width="90" align="right">
            <template slot-scope="{ row }">
              <span :style="{ color: row.failRequests > 0 ? '#F56C6C' : '#909399' }">
                {{ row.failRequests.toLocaleString() }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="成功率" width="100" align="center">
            <template slot-scope="{ row }">
              <el-tag :type="rate(row) >= 95 ? 'success' : rate(row) >= 80 ? 'warning' : 'danger'" size="mini">
                {{ rate(row) }}%
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="总 Tokens" prop="totalTokens" width="130" align="right">
            <template slot-scope="{ row }">{{ row.totalTokens.toLocaleString() }}</template>
          </el-table-column>
          <el-table-column label="费用 (¥)" prop="totalCost" width="110" align="right">
            <template slot-scope="{ row }">
              <span style="color:#E6A23C">¥ {{ Number(row.totalCost || 0).toFixed(4) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="平均耗时" prop="avgDurationMs" width="110" align="right">
            <template slot-scope="{ row }">{{ row.avgDurationMs ? row.avgDurationMs + ' ms' : '-' }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getChannelStat } from '@/api/stat'

export default {
  name: 'ChannelStat',
  data() {
    return {
      days: 7,
      loading: false,
      list: [],
      pieInstance: null,
      barInstance: null
    }
  },
  mounted() {
    this.load()
  },
  beforeDestroy() {
    this.pieInstance && this.pieInstance.dispose()
    this.barInstance && this.barInstance.dispose()
  },
  methods: {
    async load() {
      this.loading = true
      try {
        this.list = await getChannelStat(this.days)
        this.$nextTick(() => this.renderCharts())
      } finally {
        this.loading = false
      }
    },
    rate(row) {
      if (!row.totalRequests) return 0
      return Math.round(row.successRequests / row.totalRequests * 100)
    },
    rateColor(row) {
      const r = this.rate(row)
      if (r >= 95) return '#67C23A'
      if (r >= 80) return '#E6A23C'
      return '#F56C6C'
    },
    renderCharts() {
      if (!this.list.length) return
      this.renderPie()
      this.renderBar()
    },
    renderPie() {
      if (!this.$refs.pieChart) return
      if (!this.pieInstance) {
        this.pieInstance = echarts.init(this.$refs.pieChart)
      }
      this.pieInstance.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} 次 ({d}%)' },
        legend: { bottom: 0, type: 'scroll' },
        series: [{
          type: 'pie',
          radius: ['40%', '68%'],
          center: ['50%', '44%'],
          label: { formatter: '{b}\n{d}%' },
          data: this.list.map(r => ({ name: r.channelName, value: r.totalRequests }))
        }]
      })
    },
    renderBar() {
      if (!this.$refs.barChart) return
      if (!this.barInstance) {
        this.barInstance = echarts.init(this.$refs.barChart)
      }
      const names  = this.list.map(r => r.channelName)
      const tokens = this.list.map(r => r.totalTokens)
      this.barInstance.setOption({
        tooltip: { trigger: 'axis', formatter: p => `${p[0].name}<br/>Tokens: ${p[0].value.toLocaleString()}` },
        grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
        xAxis: { type: 'category', data: names, axisLabel: { interval: 0, rotate: names.length > 4 ? 15 : 0 } },
        yAxis: { type: 'value', name: 'Tokens' },
        series: [{
          type: 'bar',
          data: tokens,
          barMaxWidth: 60,
          itemStyle: { borderRadius: [4, 4, 0, 0] },
          label: { show: true, position: 'top', formatter: p => p.value > 0 ? p.value.toLocaleString() : '' }
        }]
      })
    }
  }
}
</script>

<style scoped>
.toolbar { display: flex; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 8px; }
.label { color: #606266; font-size: 14px; }

.cards { margin-bottom: 16px; }
.channel-card { margin-bottom: 16px; }
.card-name { font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 12px; }
.card-rows { display: flex; flex-direction: column; gap: 6px; }
.card-row { display: flex; justify-content: space-between; font-size: 13px; }
.card-label { color: #909399; }
.card-val { font-weight: 500; color: #303133; }

.charts { margin-top: 4px; }
.chart-box { height: 300px; }
</style>
