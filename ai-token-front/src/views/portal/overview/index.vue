<template>
  <div>
    <div class="page-title">我的概览</div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">今日调用次数</div>
          <div class="stat-value primary">{{ overview.todayCalls }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">今日消耗 Tokens</div>
          <div class="stat-value success">{{ overview.todayTokens | numFmt }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">今日费用（元）</div>
          <div class="stat-value warning">{{ overview.todayCost }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">账户余额（元）</div>
          <div class="stat-value" :class="remainingClass">
            {{ (overview.remainingMoney || 0) | moneyFmt }}
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-label">累计调用次数</div>
          <div class="stat-value">{{ overview.totalCalls }}</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-label">累计消耗 Tokens</div>
          <div class="stat-value">{{ overview.totalTokens | numFmt }}</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-label">累计费用（元）</div>
          <div class="stat-value">{{ overview.totalCost }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16" style="margin-top:16px">
      <!-- 渠道费用饼图 -->
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <div slot="header" class="chart-header">
            <span>渠道费用占比</span>
            <el-radio-group v-model="costPeriod" size="mini" @change="loadCostChart">
              <el-radio-button label="1">今日</el-radio-button>
              <el-radio-button label="7">本周</el-radio-button>
              <el-radio-button label="30">本月</el-radio-button>
            </el-radio-group>
          </div>
          <div ref="costChart" v-show="!costEmpty" class="chart-box" />
          <div v-if="costEmpty" class="chart-empty">暂无数据</div>
        </el-card>
      </el-col>

      <!-- Token 趋势柱图 -->
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <div slot="header" class="chart-header">
            <span>Token 使用趋势</span>
            <el-radio-group v-model="trendPeriod" size="mini" @change="loadTrendChart">
              <el-radio-button label="7">近 7 天</el-radio-button>
              <el-radio-button label="30">近 30 天</el-radio-button>
            </el-radio-group>
          </div>
          <div ref="trendChart" v-show="!trendEmpty" class="chart-box" />
          <div v-if="trendEmpty" class="chart-empty">暂无数据</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="6">
        <div class="stat-card center">
          <i class="el-icon-document card-icon" />
          <div class="stat-label" style="margin-top:8px">系统公告</div>
          <div class="stat-value" style="font-size:14px;color:#909399">查看系统公告</div>
          <el-button type="text" @click="showAnnouncement=true">查看公告 →</el-button>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card center">
          <i class="el-icon-key card-icon" />
          <div class="stat-label" style="margin-top:8px">我的 API 密钥</div>
          <div class="stat-value primary">{{ overview.apiKeyCount }}</div>
          <el-button type="text" @click="$router.push('/portal/mykeys')">管理密钥 →</el-button>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card center">
          <i class="el-icon-document card-icon" />
          <div class="stat-label" style="margin-top:8px">查看调用记录</div>
          <div class="stat-value" style="font-size:14px;color:#909399">了解每次请求详情</div>
          <el-button type="text" @click="$router.push('/portal/mylogs')">查看记录 →</el-button>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card center">
          <i class="el-icon-user card-icon" />
          <div class="stat-label" style="margin-top:8px">个人中心</div>
          <div class="stat-value" style="font-size:14px;color:#909399">修改密码/实名信息</div>
          <el-button type="text" @click="$router.push('/portal/profile')">进入个人中心 →</el-button>
        </div>
      </el-col>
    </el-row>

    <!-- 系统公告弹窗 -->
    <el-dialog
      :title="'📢 ' + (announcement ? announcement.title : '系统公告')"
      :visible.sync="showAnnouncement"
      width="700px"
      :close-on-click-modal="false"
      @close="closeAnnouncement"
    >
      <div v-if="announcement" class="notice-body">
        <div class="notice-content" v-html="announcement.content" />
        <div class="notice-footer">
          <span>发布人：<strong>{{ announcement.publisher }}</strong></span>
          <span style="margin-left:24px">{{ announcement.createdAt }}</span>
        </div>
      </div>
      <div v-else class="notice-body" style="text-align:center;color:#909399;padding:20px 0">暂无公告</div>
      <div slot="footer">
        <el-checkbox v-model="neverShow" style="margin-right:auto;float:left;line-height:40px">不再提示</el-checkbox>
        <el-button type="primary" @click="closeAnnouncement">我知道了</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getOverview, getChannelCostChart, getTrendChart } from '@/api/my'
import { getHomeAnnouncement } from '@/api/announcement'

export default {
  name: 'PortalOverview',
  filters: {
    numFmt(v) {
      if (v == null) return '0'
      return Number(v).toLocaleString()
    },
    moneyFmt(v) {
      if (v == null) return '不限制'
      return Number(v).toFixed(4)
    }
  },
  data() {
    return {
      overview: {
        todayCalls: 0, todayTokens: 0, todayCost: '0.000000',
        totalCalls: 0, totalTokens: 0, totalCost: '0.000000',
        apiKeyCount: 0, moneyQuota: null, usedMoney: null, remainingMoney: null
      },
      costPeriod: '1',
      trendPeriod: '7',
      costChart: null,
      trendChart: null,
      costEmpty: false,
      trendEmpty: false,
      showAnnouncement: false,
      neverShow: false,
      announcement: null
    }
  },
  computed: {
    remainingClass() {
      const r = this.overview.remainingMoney
      if (r == null) return 'stat-value success'  // 不限制 = 绿色
      const v = Number(r)
      if (v <= 0)  return 'stat-value danger'
      if (v <= 1)  return 'stat-value warning'
      return 'stat-value success'
    }
  },
  mounted() {
    this.load()
    this.loadAnnouncement()
  },
  beforeDestroy() {
    if (this.costChart) this.costChart.dispose()
    if (this.trendChart) this.trendChart.dispose()
  },
  methods: {
    async load() {
      this.overview = await getOverview()
      this.loadCostChart()
      this.loadTrendChart()
    },
    async loadAnnouncement() {
      try {
        this.announcement = await getHomeAnnouncement()
        if (this.announcement && !localStorage.getItem(`notice_${this.announcement.id}_hidden`)) {
          this.showAnnouncement = true
        }
      } catch (e) { /* no announcement */ }
    },
    closeAnnouncement() {
      if (this.neverShow && this.announcement) {
        localStorage.setItem(`notice_${this.announcement.id}_hidden`, '1')
      }
      this.showAnnouncement = false
    },
    async loadCostChart() {
      const data = await getChannelCostChart(Number(this.costPeriod))
      this.costEmpty = !data || data.length === 0
      if (this.costEmpty) return

      await this.$nextTick()
      if (!this.costChart) {
        this.costChart = echarts.init(this.$refs.costChart)
      }
      this.costChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
        legend: { bottom: 0, type: 'scroll' },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['50%', '45%'],
          data: data.map(d => ({ name: d.channelName, value: Number(d.totalCost).toFixed(6) })),
          label: { formatter: '{b}\n¥{c}' }
        }]
      })
    },
    async loadTrendChart() {
      const data = await getTrendChart(Number(this.trendPeriod))
      this.trendEmpty = !data || data.length === 0
      if (this.trendEmpty) return

      await this.$nextTick()
      if (!this.trendChart) {
        this.trendChart = echarts.init(this.$refs.trendChart)
      }
      this.trendChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['Tokens', '费用(元)'], bottom: 0 },
        grid: { left: 50, right: 60, bottom: 40, top: 20 },
        xAxis: { type: 'category', data: data.map(d => d.statDate), axisLabel: { rotate: 30 } },
        yAxis: [
          { type: 'value', name: 'Tokens', axisLabel: { formatter: v => v >= 1000 ? (v/1000).toFixed(0)+'k' : v } },
          { type: 'value', name: '元', axisLabel: { formatter: v => '¥'+v } }
        ],
        series: [
          {
            name: 'Tokens', type: 'bar', yAxisIndex: 0,
            data: data.map(d => d.totalTokens),
            itemStyle: { color: '#409EFF' }
          },
          {
            name: '费用(元)', type: 'line', yAxisIndex: 1,
            data: data.map(d => Number(d.totalCost).toFixed(6)),
            itemStyle: { color: '#E6A23C' },
            lineStyle: { width: 2 }
          }
        ]
      })
    }
  }
}
</script>
<style scoped>
.page-title { 
  font-size: 22px; 
  font-weight: 600; 
  color: #303133; 
  margin-bottom: 24px; 
}

.stat-row { 
  margin-bottom: 16px; 
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  height: 120px; /* 固定卡片高度，保持统一 */
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.stat-card.center { 
  text-align: center; 
  height: 140px; /* 中间快捷入口卡片稍高一些，容纳按钮 */
}

.stat-label { 
  font-size: 15px; 
  color: #909399; 
  margin-bottom: 10px; 
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tip-icon { 
  margin-left: 4px; 
  color: #c0c4cc; 
  cursor: default; 
  font-size: 15px; 
  vertical-align: middle; 
}

.stat-value { 
  font-size: 28px; 
  font-weight: 700; 
  color: #303133; 
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stat-value.primary { color: #409EFF; }
.stat-value.success { color: #67C23A; }
.stat-value.warning { color: #E6A23C; }
.stat-value.danger  { color: #F56C6C; }

/* 余额为不限制时的样式 */
.stat-value:has(> .unlimited) {
  font-size: 22px;
  color: #67C23A;
}

.money-total { 
  font-size: 16px; 
  font-weight: 400; 
  color: #909399; 
  margin-left: 6px; 
}

.card-icon { 
  font-size: 36px; 
  color: #c0c4cc; 
  margin-bottom: 8px;
}

/* 图表卡片 */
.chart-card { 
  border-radius: 8px; 
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 16px;
  font-weight: 500;
}

.chart-box { 
  width: 100%; 
  height: 300px; 
}

.chart-empty {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 16px;
}

/* 公告弹窗 - 放大字体 */
::v-deep .el-dialog {
  border-radius: 12px;
}

::v-deep .el-dialog__title {
  font-size: 22px;
  font-weight: 600;
}

::v-deep .el-dialog__body {
  padding: 20px 30px;
}

.notice-body { 
  font-size: 18px; 
  color: #303133; 
  line-height: 2;
}

.notice-tag {
  display: inline-block;
  background: #fdf6ec;
  color: #E6A23C;
  border: 1px solid #faecd8;
  border-radius: 6px;
  font-size: 16px;
  padding: 4px 14px;
  margin-bottom: 20px;
}

.notice-section { 
  margin: 24px 0 12px; 
  font-size: 20px; 
  font-weight: 600;
  color: #303133; 
}

.notice-list { 
  padding-left: 28px; 
  margin: 0 0 12px; 
  color: #606266; 
  font-size: 18px;
}

.notice-list li { 
  margin-bottom: 10px; 
  line-height: 1.8;
}

.notice-list li strong {
  color: #409EFF;
  font-weight: 600;
}

.notice-footer {
  margin-top: 30px;
  padding-top: 18px;
  border-top: 1px solid #f0f0f0;
  font-size: 18px;
  color: #909399;
  display: flex;
  justify-content: flex-start;
  align-items: center;
}

.notice-footer strong {
  color: #303133;
  font-size: 19px;
}

/* 弹窗底部按钮区域 */
::v-deep .el-dialog__footer {
  padding: 20px 30px;
  border-top: 1px solid #f0f0f0;
}

::v-deep .el-dialog__footer .el-checkbox {
  font-size: 18px;
}

::v-deep .el-dialog__footer .el-checkbox__label {
  font-size: 18px;
}

::v-deep .el-dialog__footer .el-button {
  font-size: 18px;
  padding: 12px 24px;
}

/* 快捷入口卡片内的按钮 */
.stat-card.center .el-button {
  font-size: 16px;
  margin-top: 8px;
}

.stat-card.center .el-button + .el-button {
  margin-left: 12px;
}

/* 确保统计卡片内容完整显示 */
.stat-card .stat-value .unlimited {
  font-size: 22px;
  color: #67C23A;
}

/* 数字格式化显示优化 */
.stat-value {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
}

.stat-value .money-total {
  font-size: 16px;
  font-weight: 400;
  color: #909399;
  margin-left: 6px;
}

/* 确保余额显示不限制时卡片高度一致 */
.stat-card:has(.stat-value:contains('不限制')) {
  height: 120px;
}

.stat-card:has(.stat-value:contains('不限制')) .stat-value {
  font-size: 24px;
  color: #67C23A;
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .stat-card {
    padding: 16px 20px;
  }
  
  .stat-value {
    font-size: 24px;
  }
  
  .notice-body {
    font-size: 16px;
  }
  
  .notice-section {
    font-size: 18px;
  }
  
  .notice-list {
    font-size: 16px;
  }
}
</style>
