<template>
  <div class="page-card">
    <div class="toolbar">
      <el-select v-model="query.caseId" placeholder="案件" clearable filterable style="width: 200px" @change="load">
        <el-option v-for="c in myCaseOptions" :key="c.id" :label="`${c.caseNo} ${c.title}`" :value="c.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px" @change="load">
        <el-option v-for="(label, key) in TIME_STATUS_MAP" :key="key" :label="label" :value="key" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 250px"
        @change="load"
      />
      <el-select v-if="store.isAdmin" v-model="query.userId" placeholder="成员" clearable filterable style="width: 120px" @change="load">
        <el-option v-for="u in users" :key="u.id" :label="u.realName" :value="u.id" />
      </el-select>
      <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
      <div class="spacer" />
      <el-button type="success" @click="openCreate"><el-icon><Plus /></el-icon>记工时</el-button>
    </div>

    <el-table v-loading="loading" :data="items">
      <el-table-column label="日期" width="110">
        <template #default="{ row }">{{ formatDate(row.workDate) }}</template>
      </el-table-column>
      <el-table-column prop="caseNo" label="案号" width="130" />
      <el-table-column prop="caseTitle" label="案件" min-width="180" show-overflow-tooltip />
      <el-table-column label="工时" width="90" align="right">
        <template #default="{ row }">{{ formatHours(row.hours) }}</template>
      </el-table-column>
      <el-table-column label="费率(元/h)" width="100" align="right">
        <template #default="{ row }">{{ row.rate ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="金额" width="110" align="right">
        <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
      </el-table-column>
      <el-table-column prop="description" label="工作内容" min-width="180" show-overflow-tooltip />
      <el-table-column v-if="store.isAdmin" prop="userName" label="成员" width="90" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="TIME_STATUS_TYPE[row.status]" size="small">{{ timeStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button v-if="canEdit(row)" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 'SUBMITTED' && row.userId === store.user?.id" link type="success" size="small" @click="submit(row)">提交</el-button>
          <el-button v-if="row.status === 'SUBMITTED' && store.isManager" link type="success" size="small" @click="review(row, true)">通过</el-button>
          <el-button v-if="row.status === 'SUBMITTED' && store.isManager" link type="warning" size="small" @click="review(row, false)">驳回</el-button>
          <el-button v-if="canEdit(row)" link type="danger" size="small" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="load"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑工时' : '记工时'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="案件" prop="caseId">
          <el-select v-model="form.caseId" filterable placeholder="选择案件" style="width: 100%">
            <el-option v-for="c in myCaseOptions" :key="c.id" :label="`${c.caseNo} ${c.title}`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" prop="workDate">
          <el-date-picker v-model="form.workDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="工时(小时)" prop="hours">
          <el-input-number v-model="form.hours" :min="0.1" :max="24" :precision="2" :step="0.5" style="width: 100%" />
        </el-form-item>
        <el-form-item label="费率(元/h)">
          <el-input-number v-model="form.rate" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="工作内容" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { myCases } from '@/api/case'
import { userOptions } from '@/api/user'
import {
  approveTimeEntry, createTimeEntry, deleteTimeEntry, pageTimeEntries, rejectTimeEntry,
  submitTimeEntry, updateTimeEntry
} from '@/api/billing'
import { useUserStore } from '@/store/user'
import { TIME_STATUS_MAP, TIME_STATUS_TYPE, timeStatusLabel } from '@/utils/dict'
import { formatDate, formatHours, formatMoney } from '@/utils/format'

const store = useUserStore()
const loading = ref(false)
const items = ref([])
const total = ref(0)
const users = ref([])
const myCaseOptions = ref([])
const dateRange = ref(null)

const query = reactive({ caseId: '', status: '', userId: '', page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach((k) => {
      if (params[k] === '' || params[k] === null || params[k] === undefined) delete params[k]
    })
    if (dateRange.value?.length === 2) {
      params.dateFrom = dateRange.value[0]
      params.dateTo = dateRange.value[1]
    }
    const data = await pageTimeEntries(params)
    items.value = data.items
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const dialogVisible = ref(false)
const editing = ref(null)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const rules = {
  caseId: [{ required: true, message: '请选择案件', trigger: 'change' }],
  workDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  hours: [{ required: true, message: '请输入工时', trigger: 'blur' }],
  description: [{ required: true, message: '请输入工作内容', trigger: 'blur' }]
}

function canEdit(row) {
  return row.status === 'SUBMITTED' && (row.userId === store.user?.id || store.isAdmin)
}

function openCreate() {
  editing.value = null
  form.value = { workDate: new Date().toISOString().slice(0, 10), hours: 1 }
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = row
  form.value = {
    caseId: row.caseId,
    workDate: row.workDate,
    hours: Number(row.hours),
    rate: row.rate ? Number(row.rate) : undefined,
    description: row.description
  }
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editing.value) {
      await updateTimeEntry(editing.value.id, form.value)
    } else {
      await createTimeEntry(form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function submit(row) {
  await submitTimeEntry(row.id)
  ElMessage.success('已提交审核')
  load()
}

async function review(row, approved) {
  if (approved) {
    await approveTimeEntry(row.id)
  } else {
    await rejectTimeEntry(row.id)
  }
  ElMessage.success(approved ? '已通过' : '已驳回')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm('确定删除该条工时记录？', '提示', { type: 'warning' })
  await deleteTimeEntry(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  load()
  users.value = await userOptions()
  const data = await myCases({ page: 1, size: 200 })
  myCaseOptions.value = data.items
})
</script>
