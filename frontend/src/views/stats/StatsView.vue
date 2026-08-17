<template>
  <div v-loading="loading">
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>案件类型分布</template>
          <div ref="typeChartRef" style="height: 320px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>近 6 个月新增案件与营收</template>
          <div ref="trendChartRef" style="height: 320px"></div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>近 30 天律师工时排行</template>
          <div ref="hoursChartRef" style="height: 320px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>数据明细</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="案件类型分布">
              <span v-for="t in stats.casesByType || []" :key="t.name" style="margin-right: 12px">
                {{ caseTypeLabel(t.name) }}：<b>{{ t.count }}</b>
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="律师工时排行">
              <div v-for="(h, idx) in stats.lawyerHoursTop || []" :key="h.userId" style="margin-bottom: 4px">
                {{ idx + 1 }}. {{ h.userName }}：{{ formatHours(h.hours) }}
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="月度趋势">
              <div v-for="m in stats.monthlyTrend || []" :key="m.month" style="margin-bottom: 2px">
                {{ m.month }}：新增 {{ m.newCases }} 件 / 营收 {{ formatMoney(m.revenue) }}
              </div>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getStats } from '@/api/dashboard'
import { caseTypeLabel } from '@/utils/dict'
import { formatHours, formatMoney } from '@/utils/format'

const loading = ref(false)
const stats = ref({})
const typeChartRef = ref()
const trendChartRef = ref()
const hoursChartRef = ref()
let charts = []

function renderCharts() {
  charts.forEach((c) => c && c.dispose())
  charts = []

  // 案件类型分布 - 饼图
  const typeData = (stats.value.casesByType || []).map((t) => ({
    name: caseTypeLabel(t.name),
    value: t.count
  }))
  if (typeData.length) {
    const chart = echarts.init(typeChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['35%', '65%'],
        data: typeData,
        label: { formatter: '{b}: {c}' }
      }]
    })
    charts.push(chart)
  }

  // 月度趋势 - 双轴图
  const months = (stats.value.monthlyTrend || []).map((m) => m.month)
  const newCases = (stats.value.monthlyTrend || []).map((m) => m.newCases)
  const revenue = (stats.value.monthlyTrend || []).map((m) => Number(m.revenue))
  if (months.length) {
    const chart = echarts.init(trendChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['新增案件', '营收(元)'] },
      grid: { left: 60, right: 60, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: months },
      yAxis: [
        { type: 'value', name: '案件数', minInterval: 1 },
        { type: 'value', name: '营收(元)' }
      ],
      series: [
        { name: '新增案件', type: 'bar', data: newCases, itemStyle: { color: '#409eff' } },
        { name: '营收(元)', type: 'line', yAxisIndex: 1, data: revenue, smooth: true, itemStyle: { color: '#67c23a' } }
      ]
    })
    charts.push(chart)
  }

  // 律师工时排行 - 横向条形图
  const hoursData = (stats.value.lawyerHoursTop || []).map((h) => ({
    name: h.userName,
    value: Number(h.hours)
  }))
  if (hoursData.length) {
    const chart = echarts.init(hoursChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 80, right: 40, top: 20, bottom: 30 },
      xAxis: { type: 'value', name: '小时' },
      yAxis: {
        type: 'category',
        data: hoursData.map((d) => d.name).reverse()
      },
      series: [{
        type: 'bar',
        data: hoursData.map((d) => d.value).reverse(),
        itemStyle: { color: '#e6a23c' }
      }]
    })
    charts.push(chart)
  }
}

onMounted(async () => {
  loading.value = true
  try {
    stats.value = await getStats()
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
  window.addEventListener('resize', resizeCharts)
})

function resizeCharts() {
  charts.forEach((c) => c && c.resize())
}

onUnmounted(() => {
  window.removeEventListener('resize', resizeCharts)
  charts.forEach((c) => c && c.dispose())
})
</script>
