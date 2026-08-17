<template>
  <div>
    <el-tabs v-model="tab" @tab-change="switchTab">
      <el-tab-pane label="我的申请" name="mine" />
      <el-tab-pane label="待我审批" name="todo" />
      <el-tab-pane label="全部" name="all" />
    </el-tabs>

    <div class="page-card">
      <div class="toolbar">
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="load">
          <el-option v-for="(label, key) in APPROVAL_STATUS_MAP" :key="key" :label="label" :value="key" />
        </el-select>
        <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
        <div class="spacer" />
        <el-button type="success" @click="openCreate"><el-icon><Plus /></el-icon>发起申请</el-button>
        <el-button v-if="store.isAdmin" @click="templateVisible = true">审批模板管理</el-button>
      </div>

      <el-table v-loading="loading" :data="items">
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small">{{ approvalTypeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="90" />
        <el-table-column prop="approverName" label="审批人" width="90" />
        <el-table-column prop="content" label="申请内容" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="APPROVAL_STATUS_TYPE[row.status]" size="small">{{ approvalStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="comment" label="审批意见" min-width="120" show-overflow-tooltip />
        <el-table-column label="申请时间" width="140">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING' && canApprove(row)" link type="success" size="small" @click="openDecision(row, true)">通过</el-button>
            <el-button v-if="row.status === 'PENDING' && canApprove(row)" link type="danger" size="small" @click="openDecision(row, false)">驳回</el-button>
            <el-button v-if="row.status === 'PENDING' && row.applicantId === store.user?.id" link type="info" size="small" @click="cancel(row)">撤销</el-button>
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
    </div>

    <!-- 发起申请 -->
    <el-dialog v-model="createVisible" title="发起审批申请" width="520px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="审批类型" prop="templateId">
          <el-select v-model="createForm.templateId" style="width: 100%" @change="onTemplateChange">
            <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="createForm.title" />
        </el-form-item>
        <el-form-item label="申请内容" prop="content">
          <el-input v-model="createForm.content" type="textarea" :rows="4" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="审批人" prop="approverId">
          <el-select v-model="createForm.approverId" style="width: 100%">
            <el-option v-for="a in approvers" :key="a.id" :label="`${a.realName}（${roleLabel(a.role)}）`" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联案件">
          <el-select v-model="createForm.caseId" clearable filterable style="width: 100%">
            <el-option v-for="c in cases" :key="c.id" :label="`${c.caseNo} ${c.title}`" :value="c.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">提交</el-button>
      </template>
    </el-dialog>

    <!-- 审批决策 -->
    <el-dialog :title="decision.approved ? '通过审批' : '驳回审批'" v-model="decisionVisible" width="440px">
      <el-form :model="decision" label-width="90px">
        <el-form-item label="审批意见" required>
          <el-input v-model="decision.comment" type="textarea" :rows="3" placeholder="请输入审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="decisionVisible = false">取消</el-button>
        <el-button :type="decision.approved ? 'success' : 'danger'" @click="submitDecision">确定</el-button>
      </template>
    </el-dialog>

    <!-- 模板管理 -->
    <el-dialog v-model="templateVisible" title="审批模板管理" width="600px">
      <div class="toolbar">
        <el-button type="primary" size="small" @click="openTemplateEdit()"><el-icon><Plus /></el-icon>新增模板</el-button>
      </div>
      <el-table :data="templates" size="small">
        <el-table-column prop="name" label="模板名称" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">{{ approvalTypeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="description" label="说明" show-overflow-tooltip />
        <el-table-column label="启用" width="70">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled" @change="(v) => toggleTemplate(row, v)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openTemplateEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="removeTemplate(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-dialog v-model="templateEditVisible" :title="templateForm.id ? '编辑模板' : '新增模板'" width="440px" append-to-body>
        <el-form :model="templateForm" label-width="80px">
          <el-form-item label="名称" required>
            <el-input v-model="templateForm.name" />
          </el-form-item>
          <el-form-item label="类型" required>
            <el-select v-model="templateForm.type" style="width: 100%">
              <el-option v-for="(label, key) in APPROVAL_TYPE_MAP" :key="key" :label="label" :value="key" />
            </el-select>
          </el-form-item>
          <el-form-item label="说明">
            <el-input v-model="templateForm.description" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="templateEditVisible = false">取消</el-button>
          <el-button type="primary" @click="saveTemplate">保存</el-button>
        </template>
      </el-dialog>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveInstance, cancelInstance, createInstance, createTemplate, deleteTemplate, listApprovers,
  listTemplates, pageInstances, rejectInstance, setTemplateEnabled, updateTemplate
} from '@/api/approval'
import { myCases } from '@/api/case'
import { useUserStore } from '@/store/user'
import {
  APPROVAL_STATUS_MAP, APPROVAL_STATUS_TYPE, APPROVAL_TYPE_MAP,
  approvalStatusLabel, approvalTypeLabel, roleLabel
} from '@/utils/dict'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const store = useUserStore()

const tab = ref(route.query.scope || 'mine')
const loading = ref(false)
const items = ref([])
const total = ref(0)
const templates = ref([])
const approvers = ref([])
const cases = ref([])
const query = reactive({ status: '', page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const params = { scope: tab.value, page: query.page, size: query.size }
    if (query.status) params.status = query.status
    const data = await pageInstances(params)
    items.value = data.items
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function switchTab() {
  query.page = 1
  load()
}

function canApprove(row) {
  return row.approverId === store.user?.id || store.isAdmin
}

// 发起申请
const createVisible = ref(false)
const createFormRef = ref()
const createForm = ref({})
const createRules = {
  templateId: [{ required: true, message: '请选择审批类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入申请内容', trigger: 'blur' }],
  approverId: [{ required: true, message: '请选择审批人', trigger: 'change' }]
}

function openCreate() {
  createForm.value = { templateId: templates.value[0]?.id, caseId: null }
  createVisible.value = true
}

function onTemplateChange(id) {
  const t = templates.value.find((x) => x.id === id)
  if (t && !createForm.value.title) {
    createForm.value.title = t.name
  }
}

async function submitCreate() {
  await createFormRef.value.validate()
  await createInstance(createForm.value)
  ElMessage.success('已提交审批')
  createVisible.value = false
  load()
}

// 审批决策
const decisionVisible = ref(false)
const decision = ref({ id: null, approved: true, comment: '' })

function openDecision(row, approved) {
  decision.value = { id: row.id, approved, comment: '' }
  decisionVisible.value = true
}

async function submitDecision() {
  if (!decision.value.comment) {
    ElMessage.warning('请输入审批意见')
    return
  }
  if (decision.value.approved) {
    await approveInstance(decision.value.id, { comment: decision.value.comment })
  } else {
    await rejectInstance(decision.value.id, { comment: decision.value.comment })
  }
  ElMessage.success('已处理')
  decisionVisible.value = false
  load()
}

async function cancel(row) {
  await ElMessageBox.confirm('确定撤销该申请？', '提示', { type: 'warning' })
  await cancelInstance(row.id)
  ElMessage.success('已撤销')
  load()
}

// 模板管理
const templateVisible = ref(false)
const templateEditVisible = ref(false)
const templateForm = ref({})

function openTemplateEdit(row) {
  templateForm.value = row
    ? { id: row.id, name: row.name, type: row.type, description: row.description }
    : { type: 'OTHER' }
  templateEditVisible.value = true
}

async function saveTemplate() {
  if (!templateForm.value.name) {
    ElMessage.warning('请输入模板名称')
    return
  }
  if (templateForm.value.id) {
    await updateTemplate(templateForm.value.id, templateForm.value)
  } else {
    await createTemplate(templateForm.value)
  }
  ElMessage.success('已保存')
  templateEditVisible.value = false
  loadTemplates()
}

async function toggleTemplate(row, enabled) {
  await setTemplateEnabled(row.id, enabled)
  row.enabled = enabled
  ElMessage.success(enabled ? '已启用' : '已停用')
}

async function removeTemplate(row) {
  await ElMessageBox.confirm(`确定删除模板「${row.name}」？`, '提示', { type: 'warning' })
  await deleteTemplate(row.id)
  ElMessage.success('已删除')
  loadTemplates()
}

async function loadTemplates() {
  templates.value = await listTemplates(store.isAdmin)
}

onMounted(async () => {
  load()
  loadTemplates()
  approvers.value = await listApprovers()
  const data = await myCases({ page: 1, size: 200 })
  cases.value = data.items
})
</script>
