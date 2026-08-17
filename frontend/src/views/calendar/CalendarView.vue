<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <el-radio-group v-model="viewType" size="small">
          <el-radio-button value="month">月视图</el-radio-button>
          <el-radio-button value="week">周视图</el-radio-button>
          <el-radio-button value="day">日视图</el-radio-button>
        </el-radio-group>
        <div style="display: flex; align-items: center; gap: 8px">
          <el-button size="small" @click="prev">上一页</el-button>
          <el-button size="small" @click="today">今天</el-button>
          <el-button size="small" @click="next">下一页</el-button>
          <span style="font-weight: 600">{{ rangeLabel }}</span>
        </div>
        <el-button type="primary" size="small" @click="openCreate"><el-icon><Plus /></el-icon>新建日程</el-button>
      </div>
    </template>

    <div class="week-head">
      <div v-for="d in weekDays" :key="d.date" class="week-head-day" :class="{ weekend: d.weekend }">
        <div>{{ d.weekday }}</div>
        <div :class="{ 'is-today': d.isToday }">{{ d.day }}</div>
      </div>
    </div>
    <div class="week-body">
      <div v-for="d in weekDays" :key="d.date" class="day-col" :class="{ weekend: d.weekend }">
        <div
          v-for="e in eventsByDate(d.date)"
          :key="e.id"
          class="event-item"
          :class="'evt-' + e.type.toLowerCase()"
          @click="openEdit(e)"
        >
          <div class="evt-time">{{ formatTime(e.startTime) }}</div>
          <div class="evt-title">{{ e.title }}</div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑日程' : '新建日程'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="(label, key) in EVENT_TYPE_MAP" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item label="关联案件">
          <el-select v-model="form.caseId" clearable filterable style="width: 100%">
            <el-option v-for="c in cases" :key="c.id" :label="`${c.caseNo} ${c.title}`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="参与人">
          <el-select v-model="form.participantIds" multiple style="width: 100%">
            <el-option v-for="u in users" :key="u.id" :label="u.realName" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button v-if="editing" type="danger" plain @click="remove">删除</el-button>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { createEvent, deleteEvent, listEvents, updateEvent } from '@/api/calendar'
import { myCases } from '@/api/case'
import { userOptions } from '@/api/user'
import { EVENT_TYPE_MAP } from '@/utils/dict'

const viewType = ref('month')
const anchor = ref(dayjs())
const events = ref([])
const users = ref([])
const cases = ref([])

const weekDays = computed(() => {
  const start = anchor.value.startOf(viewType.value === 'month' ? 'month' : viewType.value === 'week' ? 'week' : 'day')
  const count = viewType.value === 'month' ? 42 : viewType.value === 'week' ? 7 : 1
  const days = []
  for (let i = 0; i < count; i++) {
    const d = start.add(i, 'day')
    days.push({
      date: d.format('YYYY-MM-DD'),
      day: d.date(),
      weekday: d.format('ddd'),
      weekend: [0, 6].includes(d.day()),
      isToday: d.isSame(dayjs(), 'day')
    })
  }
  return days
})

const rangeLabel = computed(() => {
  if (viewType.value === 'month') return anchor.value.format('YYYY年MM月')
  if (viewType.value === 'week') {
    return `${weekDays.value[0].date} ~ ${weekDays.value[6].date}`
  }
  return anchor.value.format('YYYY-MM-DD')
})

function eventsByDate(date) {
  return events.value.filter((e) => dayjs(e.startTime).format('YYYY-MM-DD') === date)
}

function formatTime(dt) {
  return dt ? dayjs(dt).format('HH:mm') : ''
}

async function load() {
  const start = weekDays.value[0].date + 'T00:00:00'
  const end = weekDays.value[weekDays.value.length - 1].date + 'T23:59:59'
  events.value = await listEvents({ start, end })
}

function prev() {
  anchor.value = anchor.value.subtract(1, viewType.value)
  load()
}

function next() {
  anchor.value = anchor.value.add(1, viewType.value)
  load()
}

function today() {
  anchor.value = dayjs()
  load()
}

const dialogVisible = ref(false)
const editing = ref(null)
const formRef = ref()
const form = ref({})
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }]
}

function openCreate() {
  editing.value = null
  form.value = {
    type: 'TASK',
    startTime: anchor.value.format('YYYY-MM-DD') + ' 09:00:00',
    endTime: anchor.value.format('YYYY-MM-DD') + ' 10:00:00',
    participantIds: []
  }
  dialogVisible.value = true
}

function openEdit(e) {
  editing.value = e
  form.value = {
    title: e.title,
    type: e.type,
    startTime: e.startTime,
    endTime: e.endTime,
    location: e.location,
    description: e.description,
    caseId: e.caseId,
    participantIds: [...(e.participantIds || [])]
  }
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  if (editing.value) {
    await updateEvent(editing.value.id, form.value)
  } else {
    await createEvent(form.value)
  }
  ElMessage.success('已保存')
  dialogVisible.value = false
  load()
}

async function remove() {
  await ElMessageBox.confirm('确定删除该日程？', '提示', { type: 'warning' })
  await deleteEvent(editing.value.id)
  ElMessage.success('已删除')
  dialogVisible.value = false
  load()
}

onMounted(async () => {
  load()
  users.value = await userOptions()
  const data = await myCases({ page: 1, size: 200 })
  cases.value = data.items
})
</script>

<style scoped>
.week-head {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 4px;
}

.week-head-day {
  text-align: center;
  padding: 8px 0;
  font-size: 13px;
  color: #606266;
}

.week-head-day .is-today {
  background: #409eff;
  color: #fff;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  line-height: 24px;
  margin: 0 auto;
}

.week-body {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.day-col {
  min-height: 480px;
  border-radius: 4px;
  padding: 4px;
  background: #fafafa;
}

.day-col.weekend {
  background: #f3f4f6;
}

.event-item {
  background: #ecf5ff;
  border-left: 3px solid #409eff;
  border-radius: 3px;
  padding: 4px 6px;
  margin-bottom: 4px;
  cursor: pointer;
  font-size: 12px;
}

.event-item.evt-court {
  background: #fef0f0;
  border-left-color: #f56c6c;
}

.event-item.evt-meeting {
  background: #ecf5ff;
  border-left-color: #409eff;
}

.event-item.evt-task {
  background: #f0f9eb;
  border-left-color: #67c23a;
}

.event-item.evt-reminder {
  background: #fdf6ec;
  border-left-color: #e6a23c;
}

.evt-time {
  font-size: 11px;
  color: #909399;
}

.evt-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
