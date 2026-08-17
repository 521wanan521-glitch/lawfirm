<template>
  <div class="page-card">
    <div class="toolbar">
      <el-select v-model="query.clientId" placeholder="客户" clearable filterable style="width: 200px" @change="load">
        <el-option v-for="c in clients" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="load">
        <el-option v-for="(label, key) in INVOICE_STATUS_MAP" :key="key" :label="label" :value="key" />
      </el-select>
      <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
      <div class="spacer" />
      <el-button v-if="store.isManager" type="success" @click="openCreate"><el-icon><Plus /></el-icon>生成账单</el-button>
    </div>

    <el-table v-loading="loading" :data="items">
      <el-table-column prop="invoiceNo" label="账单编号" width="130" />
      <el-table-column prop="clientName" label="客户" min-width="150" show-overflow-tooltip />
      <el-table-column prop="userName" label="开票人" width="100" />
      <el-table-column label="开票日期" width="110">
        <template #default="{ row }">{{ formatDate(row.issueDate) }}</template>
      </el-table-column>
      <el-table-column label="到期日" width="110">
        <template #default="{ row }">{{ formatDate(row.dueDate) }}</template>
      </el-table-column>
      <el-table-column label="金额" width="130" align="right">
        <template #default="{ row }">
          <b>{{ formatMoney(row.totalAmount) }}</b>
        </template>
      </el-table-column>
      <el-table-column label="工时数" width="80" align="center">
        <template #default="{ row }">{{ row.timeEntryCount }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="INVOICE_STATUS_TYPE[row.status]" size="small">{{ invoiceStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          <el-button v-if="row.status === 'DRAFT' && store.isManager" link type="primary" size="small" @click="changeStatus(row, 'ISSUED')">开票</el-button>
          <el-button v-if="row.status === 'ISSUED' && store.isManager" link type="success" size="small" @click="changeStatus(row, 'PAID')">收款</el-button>
          <el-button v-if="(row.status === 'DRAFT' || row.status === 'ISSUED') && store.isManager" link type="warning" size="small" @click="changeStatus(row, 'VOID')">作废</el-button>
          <el-button v-if="row.status === 'DRAFT' && store.isManager" link type="danger" size="small" @click="remove(row)">删除</el-button>
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

    <!-- 生成账单 -->
    <el-dialog v-model="createVisible" title="生成账单" width="720px">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="客户" required>
          <el-select v-model="createForm.clientId" filterable placeholder="选择客户" style="width: 100%">
            <el-option v-for="c in clients" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开票日期" required>
          <el-date-picker v-model="createForm.issueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" />
        </el-form-item>
        <el-form-item label="选择工时">
          <el-table
            ref="timeTableRef"
            :data="approvedEntries"
            max-height="300"
            size="small"
            border
            @selection-change="onSelectionChange"
          >
            <el-table-column type="selection" width="40" />
            <el-table-column prop="caseNo" label="案号" width="120" />
            <el-table-column prop="caseTitle" label="案件" show-overflow-tooltip />
            <el-table-column label="日期" width="100">
              <template #default="{ row }">{{ formatDate(row.workDate) }}</template>
            </el-table-column>
            <el-table-column label="工时" width="80" align="right">
              <template #default="{ row }">{{ formatHours(row.hours) }}</template>
            </el-table-column>
            <el-table-column label="金额" width="100" align="right">
              <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
            </el-table-column>
          </el-table>
          <div class="text-muted" style="margin-top: 6px">
            仅显示「已审核」工时；所选工时需属于同一客户。
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveInvoice">生成</el-button>
      </template>
    </el-dialog>

    <!-- 账单详情 -->
    <el-dialog v-model="detailVisible" title="账单详情" width="680px">
      <template v-if="current">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="账单编号">{{ current.invoiceNo }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ current.clientName }}</el-descriptions-item>
          <el-descriptions-item label="开票人">{{ current.userName }}</el-descriptions-item>
          <el-descriptions-item label="开票日期">{{ formatDate(current.issueDate) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="INVOICE_STATUS_TYPE[current.status]" size="small">{{ invoiceStatusLabel(current.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总额">
            <b style="color: #f56c6c">{{ formatMoney(current.totalAmount) }}</b>
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ current.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageClients } from '@/api/client'
import {
  createInvoice, deleteInvoice, pageInvoices, pageTimeEntries, updateInvoiceStatus
} from '@/api/billing'
import { useUserStore } from '@/store/user'
import { INVOICE_STATUS_MAP, INVOICE_STATUS_TYPE, invoiceStatusLabel } from '@/utils/dict'
import { formatDate, formatHours, formatMoney } from '@/utils/format'

const store = useUserStore()
const loading = ref(false)
const items = ref([])
const total = ref(0)
const clients = ref([])
const query = reactive({ clientId: '', status: '', page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach((k) => {
      if (params[k] === '' || params[k] === null || params[k] === undefined) delete params[k]
    })
    const data = await pageInvoices(params)
    items.value = data.items
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const createVisible = ref(false)
const saving = ref(false)
const approvedEntries = ref([])
const selectedEntries = ref([])
const createForm = ref({ clientId: '', issueDate: '', remark: '' })

async function openCreate() {
  createForm.value = { clientId: '', issueDate: new Date().toISOString().slice(0, 10), remark: '' }
  selectedEntries.value = []
  createVisible.value = true
  const data = await pageTimeEntries({ status: 'APPROVED', page: 1, size: 200 })
  approvedEntries.value = data.items
}

function onSelectionChange(rows) {
  selectedEntries.value = rows
}

async function saveInvoice() {
  if (!createForm.value.clientId) {
    ElMessage.warning('请选择客户')
    return
  }
  if (!createForm.value.issueDate) {
    ElMessage.warning('请选择开票日期')
    return
  }
  if (!selectedEntries.value.length) {
    ElMessage.warning('请选择工时记录')
    return
  }
  saving.value = true
  try {
    await createInvoice({
      clientId: createForm.value.clientId,
      issueDate: createForm.value.issueDate,
      remark: createForm.value.remark,
      timeEntryIds: selectedEntries.value.map((e) => e.id)
    })
    ElMessage.success('账单已生成')
    createVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const detailVisible = ref(false)
const current = ref(null)

function openDetail(row) {
  current.value = row
  detailVisible.value = true
}

async function changeStatus(row, status) {
  const labels = { ISSUED: '开票', PAID: '标记收款', VOID: '作废' }
  await ElMessageBox.confirm(`确定${labels[status]}该账单？`, '提示', { type: 'warning' })
  await updateInvoiceStatus(row.id, { status })
  ElMessage.success('操作成功')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm('确定删除该草稿账单？关联工时将恢复为已审核。', '提示', { type: 'warning' })
  await deleteInvoice(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  load()
  const data = await pageClients({ page: 1, size: 100 })
  clients.value = data.items
})
</script>
