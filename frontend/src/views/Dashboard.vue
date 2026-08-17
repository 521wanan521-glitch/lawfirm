<template>
  <div>
    <el-row :gutter="16">
      <el-col v-for="card in statCards" :key="card.label" :xs="12" :sm="8" :md="6" :lg="4">
        <el-card shadow="hover" class="stat-card" :body-style="{ padding: '16px' }">
          <div class="icon" :style="{ background: card.color }">
            <el-icon :size="26"><component :is="card.icon" /></el-icon>
          </div>
          <div>
            <div class="value">{{ card.value }}</div>
            <div class="label">{{ card.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>最近案件</template>
          <el-table :data="summary.recentCases || []" size="small" @row-click="goCase">
            <el-table-column prop="caseNo" label="案号" width="120" />
            <el-table-column prop="title" label="案件名称" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="CASE_STATUS_TYPE[row.status]" size="small">
                  {{ caseStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="leadLawyerName" label="主办律师" width="110" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>待办提醒</template>
          <div v-if="todos.length" class="todo-list">
            <div v-for="todo in todos" :key="todo.label" class="todo-item" @click="todo.action">
              <el-badge :value="todo.value" :type="todo.type" :max="99">
                <el-button size="small" plain>{{ todo.label }}</el-button>
              </el-badge>
            </div>
          </div>
          <el-empty v-else description="暂无待办" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getSummary } from '@/api/dashboard'
import { useUserStore } from '@/store/user'
import { caseStatusLabel, CASE_STATUS_TYPE } from '@/utils/dict'

const router = useRouter()
const store = useUserStore()
const summary = ref({})

const statCards = computed(() => [
  { label: '案件总数', value: summary.value.totalCases ?? '-', icon: 'Files', color: '#409eff' },
  { label: '办理中', value: summary.value.activeCases ?? '-', icon: 'Loading', color: '#67c23a' },
  { label: '已结案', value: summary.value.closedCases ?? '-', icon: 'CircleCheck', color: '#909399' },
  { label: '客户总数', value: summary.value.totalClients ?? '-', icon: 'User', color: '#e6a23c' },
  { label: '待审批', value: summary.value.pendingApprovals ?? '-', icon: 'Stamp', color: '#f56c6c' },
  { label: '待审核工时', value: summary.value.pendingTimeEntries ?? '-', icon: 'Clock', color: '#9c27b0' },
  { label: '我的在办案件', value: summary.value.myOpenCases ?? '-', icon: 'Suitcase', color: '#00bcd4' },
  { label: '本月营收(元)', value: summary.value.revenueThisMonth ?? '-', icon: 'Money', color: '#4caf50' }
])

const todos = computed(() => {
  const list = []
  if (summary.value.pendingApprovals > 0) {
    list.push({
      label: `${summary.value.pendingApprovals} 条审批待处理`,
      value: summary.value.pendingApprovals,
      type: 'danger',
      action: () => router.push('/approvals?scope=todo')
    })
  }
  if (store.isManager && summary.value.pendingTimeEntries > 0) {
    list.push({
      label: `${summary.value.pendingTimeEntries} 条工时待审核`,
      value: summary.value.pendingTimeEntries,
      type: 'warning',
      action: () => router.push('/billing/time')
    })
  }
  if (summary.value.upcomingEvents > 0) {
    list.push({
      label: `${summary.value.upcomingEvents} 项日程即将到来`,
      value: summary.value.upcomingEvents,
      type: 'primary',
      action: () => router.push('/calendar')
    })
  }
  return list
})

function goCase(row) {
  router.push(`/cases/${row.id}`)
}

onMounted(async () => {
  summary.value = await getSummary()
})
</script>

<style scoped>
.todo-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.todo-item {
  cursor: pointer;
}
</style>
