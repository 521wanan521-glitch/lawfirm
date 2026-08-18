<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索案号 / 案件名称"
        clearable
        style="width: 220px"
        @keyup.enter="load"
        @clear="load"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="load">
        <el-option v-for="(label, key) in CASE_STATUS_MAP" :key="key" :label="label" :value="key" />
      </el-select>
      <el-select v-model="query.type" placeholder="案件类型" clearable style="width: 130px" @change="load">
        <el-option v-for="(label, key) in CASE_TYPE_MAP" :key="key" :label="label" :value="key" />
      </el-select>
      <el-select v-model="query.priority" placeholder="优先级" clearable style="width: 110px" @change="load">
        <el-option v-for="(label, key) in PRIORITY_MAP" :key="key" :label="label" :value="key" />
      </el-select>
      <el-select v-model="query.leadLawyerId" placeholder="主办律师" clearable filterable style="width: 130px" @change="load">
        <el-option v-for="u in users" :key="u.id" :label="u.realName" :value="u.id" />
      </el-select>
      <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
      <div class="spacer" />
      <el-button type="success" @click="openCreate"><el-icon><Plus /></el-icon>立案</el-button>
    </div>

    <el-table v-loading="loading" :data="items" @row-click="(row) => $router.push(`/cases/${row.id}`)" style="cursor: pointer">
      <el-table-column prop="caseNo" label="案号" width="130" />
      <el-table-column prop="title" label="案件名称" min-width="200" show-overflow-tooltip />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ caseTypeLabel(row.type) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="CASE_STATUS_TYPE[row.status]" size="small">{{ caseStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="优先级" width="80">
        <template #default="{ row }">
          <el-tag :type="PRIORITY_TYPE[row.priority]" size="small">{{ priorityLabel(row.priority) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="clientName" label="客户" width="140" show-overflow-tooltip />
      <el-table-column prop="leadLawyerName" label="主办律师" width="100" />
      <el-table-column label="标的额(元)" width="110" align="right">
        <template #default="{ row }">{{ formatMoney(row.caseAmount) }}</template>
      </el-table-column>
      <el-table-column label="立案日期" width="110">
        <template #default="{ row }">{{ formatDate(row.filingDate) }}</template>
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

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑案件' : '立案'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="24">
            <el-form-item label="案件名称" prop="title">
              <el-input v-model="form.title" placeholder="请输入案件名称" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户" prop="clientId">
              <el-select v-model="form.clientId" filterable placeholder="选择客户" style="width: 100%">
                <el-option v-for="c in clients" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="案件类型" prop="type">
              <el-select v-model="form.type" style="width: 100%">
                <el-option v-for="(label, key) in CASE_TYPE_MAP" :key="key" :label="label" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主办律师" prop="leadLawyerId">
              <el-select v-model="form.leadLawyerId" style="width: 100%">
                <el-option v-for="u in users" :key="u.id" :label="u.realName" :value="u.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="协办律师">
              <el-select v-model="form.coLawyerIds" multiple style="width: 100%">
                <el-option v-for="u in users" :key="u.id" :label="u.realName" :value="u.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级">
              <el-select v-model="form.priority" style="width: 100%">
                <el-option v-for="(label, key) in PRIORITY_MAP" :key="key" :label="label" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="立案日期">
              <el-date-picker v-model="form.filingDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="受理法院">
              <el-input v-model="form.court" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标的额(元)">
              <el-input-number v-model="form.caseAmount" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收费金额(元)">
              <el-input-number v-model="form.fee" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="案情摘要">
              <el-input v-model="form.description" type="textarea" :rows="3" maxlength="2000" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
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
import { ElMessage } from 'element-plus'
import { createCase, pageCases, updateCase } from '@/api/case'
import { pageClients } from '@/api/client'
import { userOptions } from '@/api/user'
import {
  CASE_STATUS_MAP, CASE_STATUS_TYPE, CASE_TYPE_MAP, PRIORITY_MAP, PRIORITY_TYPE,
  caseStatusLabel, caseTypeLabel, priorityLabel
} from '@/utils/dict'
import { formatDate, formatMoney } from '@/utils/format'

const loading = ref(false)
const items = ref([])
const total = ref(0)
const users = ref([])
const clients = ref([])

const query = reactive({ keyword: '', status: '', type: '', priority: '', leadLawyerId: '', page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    if (!params.keyword) delete params.keyword
    if (!params.status) delete params.status
    if (!params.type) delete params.type
    if (!params.priority) delete params.priority
    if (!params.leadLawyerId) delete params.leadLawyerId
    const data = await pageCases(params)
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
  clientId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  title: [{ required: true, message: '请输入案件名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择案件类型', trigger: 'change' }],
  leadLawyerId: [{ required: true, message: '请选择主办律师', trigger: 'change' }]
}

function openCreate() {
  editing.value = null
  form.value = { coLawyerIds: [], priority: 'MEDIUM', type: 'CIVIL' }
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = row
  form.value = {
    clientId: row.clientId,
    title: row.title,
    type: row.type,
    priority: row.priority,
    leadLawyerId: row.leadLawyerId,
    coLawyerIds: [...(row.coLawyerIds || [])],
    court: row.court,
    caseAmount: row.caseAmount,
    filingDate: row.filingDate,
    description: row.description,
    fee: row.fee
  }
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editing.value) {
      await updateCase(editing.value.id, form.value)
    } else {
      await createCase(form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  load()
  const [u, c] = await Promise.all([userOptions(), pageClients({ page: 1, size: 100 })])
  users.value = u
  clients.value = c.items
})

defineExpose({ openEdit })
</script>
