<template>
  <div v-loading="loading">
    <el-page-header @back="$router.back()" style="margin-bottom: 16px">
      <template #content>
        <b>{{ detail.title }}</b>
        <el-tag :type="CASE_STATUS_TYPE[detail.status]" size="small" style="margin-left: 8px">
          {{ caseStatusLabel(detail.status) }}
        </el-tag>
      </template>
      <template #extra>
        <el-button size="small" @click="openStatusDialog">变更状态</el-button>
        <el-button size="small" type="primary" plain @click="openEditDialog">编辑</el-button>
        <el-button size="small" type="danger" plain @click="onDelete">删除</el-button>
        <el-button size="small" @click="$router.push('/documents?caseId=' + detail.id)">查看文档</el-button>
      </template>
    </el-page-header>

    <el-row :gutter="16">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>基本信息</template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="案号">{{ detail.caseNo }}</el-descriptions-item>
            <el-descriptions-item label="案件类型">{{ caseTypeLabel(detail.type) }}</el-descriptions-item>
            <el-descriptions-item label="客户">
              <el-link type="primary" @click="$router.push('/clients/' + detail.clientId)">{{ detail.clientName }}</el-link>
            </el-descriptions-item>
            <el-descriptions-item label="优先级">
              <el-tag :type="PRIORITY_TYPE[detail.priority]" size="small">{{ priorityLabel(detail.priority) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="主办律师">{{ detail.leadLawyerName }}</el-descriptions-item>
            <el-descriptions-item label="协办律师">{{ (detail.coLawyerNames || []).join('、') || '-' }}</el-descriptions-item>
            <el-descriptions-item label="受理法院">{{ detail.court || '-' }}</el-descriptions-item>
            <el-descriptions-item label="立案日期">{{ formatDate(detail.filingDate) }}</el-descriptions-item>
            <el-descriptions-item label="标的额">{{ formatMoney(detail.caseAmount) }}</el-descriptions-item>
            <el-descriptions-item label="收费金额">{{ formatMoney(detail.fee) }}</el-descriptions-item>
            <el-descriptions-item label="结案日期">{{ formatDate(detail.closeDate) }}</el-descriptions-item>
            <el-descriptions-item label="办理结果" :span="2">{{ detail.result || '-' }}</el-descriptions-item>
            <el-descriptions-item label="案情摘要" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>案件进程</span>
              <el-button size="small" type="primary" @click="progressVisible = true">添加进程</el-button>
            </div>
          </template>
          <el-timeline v-if="progressList.length">
            <el-timeline-item
              v-for="p in progressList"
              :key="p.id"
              :timestamp="formatDateTime(p.createdAt)"
              placement="top"
              :type="p.newStatus ? 'primary' : ''"
            >
              <div>{{ p.content }}</div>
              <div class="text-muted">{{ p.userName }}
                <el-tag v-if="p.newStatus" size="small" style="margin-left: 6px">{{ caseStatusLabel(p.newStatus) }}</el-tag>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无进程记录" :image-size="60" />
          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="progressQuery.page"
              :page-size="progressQuery.size"
              :total="progressTotal"
              layout="total, prev, pager, next"
              small
              @current-change="loadProgress"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 变更状态 -->
    <el-dialog v-model="statusVisible" title="变更案件状态" width="460px">
      <el-form :model="statusForm" label-width="90px">
        <el-form-item label="新状态">
          <el-select v-model="statusForm.status" style="width: 100%">
            <el-option v-for="(label, key) in CASE_STATUS_MAP" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="statusForm.status === 'CLOSED'" label="结案日期">
          <el-date-picker v-model="statusForm.closeDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="statusForm.status === 'CLOSED'" label="办理结果">
          <el-input v-model="statusForm.result" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="primary" @click="saveStatus">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加进程 -->
    <el-dialog v-model="progressVisible" title="添加进程记录" width="460px">
      <el-form ref="progressFormRef" :model="progressForm" :rules="progressRules" label-width="90px">
        <el-form-item label="日期" prop="progressDate">
          <el-date-picker v-model="progressForm.progressDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="progressForm.content" type="textarea" :rows="4" maxlength="2000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="progressVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProgress">保存</el-button>
      </template>
    </el-dialog>

    <!-- 编辑（复用 CaseList 弹窗） -->
    <CaseListDialog v-if="editVisible" :case-row="detail" @close="editVisible = false" @saved="reload" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { addProgress, deleteCase, getCase, pageProgress, updateCaseStatus } from '@/api/case'
import {
  CASE_STATUS_MAP, CASE_STATUS_TYPE, CASE_TYPE_MAP, PRIORITY_TYPE,
  caseStatusLabel, caseTypeLabel, priorityLabel
} from '@/utils/dict'
import { formatDate, formatDateTime, formatMoney } from '@/utils/format'
import CaseListDialog from './CaseListDialog.vue'

const route = useRoute()
const router = useRouter()
const caseId = route.params.id

const loading = ref(false)
const detail = ref({})
const progressList = ref([])
const progressTotal = ref(0)
const progressQuery = reactive({ page: 1, size: 10 })

const statusVisible = ref(false)
const statusForm = ref({ status: '', closeDate: '', result: '' })

const progressVisible = ref(false)
const progressFormRef = ref()
const progressForm = ref({ progressDate: dayjs().format('YYYY-MM-DD'), content: '' })
const progressRules = {
  progressDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const editVisible = ref(false)

async function load() {
  loading.value = true
  try {
    detail.value = await getCase(caseId)
  } finally {
    loading.value = false
  }
}

async function loadProgress() {
  const data = await pageProgress(caseId, progressQuery)
  progressList.value = data.items
  progressTotal.value = data.total
}

function openStatusDialog() {
  statusForm.value = { status: detail.value.status, closeDate: detail.value.closeDate, result: detail.value.result }
  statusVisible.value = true
}

async function saveStatus() {
  await updateCaseStatus(caseId, statusForm.value)
  ElMessage.success('状态已更新')
  statusVisible.value = false
  reload()
}

function openEditDialog() {
  editVisible.value = true
}

async function saveProgress() {
  await progressFormRef.value.validate()
  await addProgress(caseId, progressForm.value)
  ElMessage.success('已记录')
  progressVisible.value = false
  progressForm.value.content = ''
  loadProgress()
}

async function onDelete() {
  await ElMessageBox.confirm('确定删除该案件？删除后不可恢复。', '提示', { type: 'warning' })
  await deleteCase(caseId)
  ElMessage.success('已删除')
  router.push('/cases')
}

function reload() {
  editVisible.value = false
  load()
  loadProgress()
}

onMounted(() => {
  load()
  loadProgress()
})
</script>
