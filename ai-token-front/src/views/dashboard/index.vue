<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="card in statCards" :key="card.label">
        <div class="stat-card" :style="{ borderTop: `3px solid ${card.color}` }">
          <div class="stat-icon" :style="{ color: card.color }">
            <i :class="card.icon" />
          </div>
          <div class="stat-body">
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-label">
              {{ card.label }}
              <el-tooltip v-if="card.tip" placement="bottom" effect="light" :content="card.tip">
                <i class="el-icon-question tip-icon" />
              </el-tooltip>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16" style="margin-top:16px">
      <!-- 趋势折线图 -->
      <el-col :span="16">
        <div class="chart-card">
          <div class="chart-header">
            <span class="chart-title">Token 用量趋势</span>
            <el-radio-group v-model="trendDays" size="mini" @change="loadTrend">
              <el-radio-button :label="7">近7天</el-radio-button>
              <el-radio-button :label="30">近30天</el-radio-button>
            </el-radio-group>
          </div>
          <div ref="trendChart" class="chart-body" />
        </div>
      </el-col>

      <!-- 模型分布饼图 -->
      <el-col :span="8">
        <div class="chart-card">
          <div class="chart-header">
            <span class="chart-title">模型 Token 占比（近30天）</span>
          </div>
          <div ref="pieChart" class="chart-body" />
        </div>
      </el-col>
    </el-row>

    <!-- 渠道健康状态 -->
    <div class="chart-card" style="margin-top:16px">
      <div class="chart-header"><span class="chart-title">渠道健康状态</span></div>
      <el-table :data="channelHealth" size="small" style="width:100%">
        <el-table-column prop="name"         label="渠道名称" />
        <el-table-column prop="type"         label="类型" width="100" />
        <el-table-column prop="healthStatus" label="健康状态" width="100">
          <template slot-scope="{ row }">
            <el-tag :type="row.healthStatus === 1 ? 'success' : 'danger'" size="mini">
              {{ row.healthStatus === 1 ? '正常' : '异常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastCheckAt" label="上次检测" width="160" />
      </el-table>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getDashboard } from '@/api/stat'
import { getChannelList } from '@/api/channel'

export default {
  name: 'Dashboard',
  data() {
    return {
      trendDays: 7,
      dashboard: {},
      channelHealth: [],
      trendChart: null,
      pieChart: null
    }
  },
  computed: {
    statCards() {
      const d = this.dashboard
      const successRate = d.todayCallCount
        ? ((d.todaySuccessCount / d.todayCallCount) * 100).toFixed(1) + '%'
        : '—'
      return [
        { label: '今日调用次数', value: d.todayCallCount  || 0, icon: 'el-icon-s-data',    color: '#409EFF' },
        { label: '今日 Token 数', value: this.fmtNum(d.todayTotalTokens), icon: 'el-icon-coin',       color: '#67C23A' },
        { label: '今日费用(元)',  value: d.todayCost       || '0.00',     icon: 'el-icon-money',      color: '#E6A23C', tip: '费用由 tokens 折算，实际价格可能略有波动，以 tokens 消耗为准。' },
        { label: '今日成功率',    value: successRate,                      icon: 'el-icon-circle-check', color: '#F56C6C' }
      ]
    }
  },
  async mounted() {
    await this.loadDashboard()
    await this.loadChannels()
    window.addEventListener('resize', this.onResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.onResize)
    this.trendChart && this.trendChart.dispose()
    this.pieChart   && this.pieChart.dispose()
  },
  methods: {
    async loadDashboard() {
      try {
        this.dashboard = await getDashboard()
        this.$nextTick(() => {
          this.initTrendChart(this.dashboard.trend || [])
          this.initPieChart(this.dashboard.modelDistribution || [])
        })
      } catch {}
    },
    async loadTrend() {
      const data = await getDashboard()
      this.initTrendChart(data.trend || [])
    },
    async loadChannels() {
      try {
        this.channelHealth = await getChannelList()
      } catch {}
    },
    initTrendChart(trend) {
      if (!this.$refs.trendChart) return
      if (!this.trendChart) {
        this.trendChart = echarts.init(this.$refs.trendChart)
      }
      this.trendChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['调用次数', 'Token 数'] },
        grid: { left: 40, right: 20, top: 36, bottom: 24 },
        xAxis: {
          type: 'category',
          data: trend.map(t => t.statDate),
          axisLabel: { fontSize: 11 }
        },
        yAxis: [
          { type: 'value', name: '调用次数', nameTextStyle: { fontSize: 11 } },
          { type: 'value', name: 'Token 数', nameTextStyle: { fontSize: 11 } }
        ],
        series: [
          {
            name: '调用次数', type: 'bar', barMaxWidth: 32,
            itemStyle: { color: '#409EFF' },
            data: trend.map(t => t.callCount)
          },
          {
            name: 'Token 数', type: 'line', yAxisIndex: 1, smooth: true,
            itemStyle: { color: '#67C23A' },
            areaStyle: { opacity: 0.1 },
            data: trend.map(t => t.totalTokens)
          }
        ]
      })
    },
    initPieChart(dist) {
      if (!this.$refs.pieChart) return
      if (!this.pieChart) {
        this.pieChart = echarts.init(this.$refs.pieChart)
      }
      this.pieChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { orient: 'vertical', left: 10, top: 'center', textStyle: { fontSize: 11 } },
        series: [{
          type: 'pie',
          radius: ['40%', '65%'],
          center: ['65%', '50%'],
          label: { show: false },
          data: dist.length
            ? dist.map(d => ({ name: d.modelName, value: d.totalTokens }))
            : [{ name: '暂无数据', value: 1 }]
        }]
      })
    },
    onResize() {
      this.trendChart && this.trendChart.resize()
      this.pieChart   && this.pieChart.resize()
    },
    fmtNum(n) {
      if (!n) return 0
      if (n >= 1e6) return (n / 1e6).toFixed(1) + 'M'
      if (n >= 1e3) return (n / 1e3).toFixed(1) + 'K'
      return n
    }
  }
}
</script>

<style scoped>
/* 统计卡片 */
.stat-row { margin-bottom: 0; }
.stat-card {
  background: #fff;
  border-radius: 4px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
}
.stat-icon { font-size: 36px; }
.stat-value { font-size: 24px; font-weight: 600; color: #303133; line-height: 1; }
.stat-label { font-size: 13px; color: #909399; margin-top: 6px; }
.tip-icon { margin-left: 4px; color: #c0c4cc; cursor: default; font-size: 13px; vertical-align: middle; }

/* 图表卡片 */
.chart-card {
  background: #fff;
  border-radius: 4px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
}
.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.chart-title { font-size: 14px; font-weight: 500; color: #303133; }
.chart-body  { height: 260px; }
</style>
